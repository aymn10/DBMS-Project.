import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class TableViewer extends JFrame {
    private JTable table;
    private JComboBox<String> filterColumn, sortColumn, sortOrder;
    private JTextField filterText;
    private TableRowSorter<TableModel> sorter;
    private String userMobile;
    private String currentTableName;
    private boolean isFavoritesView; // Flag to indicate if viewing a favorites table

    private JCheckBox[] columnChecks;
    private java.util.List<TableColumn> allTableColumns;

    /**
     * Original constructor for generic table viewing.
     */
    public TableViewer(String tableName) {
        this(tableName, null);
    }

    /**
     * Constructor for viewing specific tables, including user-filtered Favorites.
     */
    public TableViewer(String tableName, String userMobile) {
        this.userMobile = userMobile;
        this.currentTableName = tableName;
        this.isFavoritesView = tableName.startsWith("Favorites_") && userMobile != null;

        setTitle("Viewing: " + tableName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout()); // Main layout

        // --- NEW: Panel for the top title ---
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5)); // Centered flow layout
        if (isFavoritesView) {
            JLabel favoritesTitle = new JLabel("⭐ My Favorites ⭐");
            favoritesTitle.setFont(new Font("Arial", Font.BOLD, 20)); // Prominent font
            titlePanel.add(favoritesTitle);
            add(titlePanel, BorderLayout.NORTH); // Add title panel to the top
        }
        // --- END NEW TITLE PANEL ---


        DefaultTableModel model = fetchTableData(tableName, userMobile);

        if (model == null || model.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "Could not load data for '" + tableName + "'.", "Load Error", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }

        table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(220, 220, 220));
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // --- Main content panel (controls + table) ---
        JPanel mainContentPanel = new JPanel(new BorderLayout());

        // --- Conditional UI and Behavior ---
        if (isFavoritesView) {
            // --- FAVORITES VIEW (like Dashboard) ---
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            allTableColumns = new ArrayList<>();
            TableColumnModel columnModel = table.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                allTableColumns.add(columnModel.getColumn(i));
            }
            applyNumericComparators(model);

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem removeFromFavoritesItem = new JMenuItem("❌ Remove from Favorites");
            popupMenu.add(removeFromFavoritesItem);
            table.setComponentPopupMenu(popupMenu);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row >= 0 && row < table.getRowCount()) {
                            table.setRowSelectionInterval(row, row);
                        } else {
                            table.clearSelection();
                        }
                    }
                }
            });
            removeFromFavoritesItem.addActionListener(e -> removeSelectedRowFromFavorites());

            // Control Panel Setup (Checkboxes, Search, Sort)
            JPanel controlPanel = new JPanel(new BorderLayout());

            JPanel columnCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            columnChecks = new JCheckBox[model.getColumnCount()];
            for (int i = 0; i < model.getColumnCount(); i++) {
                final int colIndex = i;
                String colName = model.getColumnName(i);
                columnChecks[i] = new JCheckBox(colName, true);
                if (colName.equalsIgnoreCase("fav_id") || colName.equalsIgnoreCase("user_mobile") || colName.equalsIgnoreCase("fav_user_mobile")) {
                    // Defer hiding
                } else {
                    columnChecks[i].addActionListener(e -> toggleColumn(colIndex));
                    columnCheckPanel.add(columnChecks[i]);
                }
            }
            JScrollPane checkScroll = new JScrollPane(columnCheckPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            checkScroll.setPreferredSize(new Dimension(800, 45));

            JPanel northControlPanel = new JPanel(new BorderLayout(10, 0));
            northControlPanel.add(checkScroll, BorderLayout.CENTER);
            controlPanel.add(northControlPanel, BorderLayout.NORTH);

            JPanel queryAndSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel tableLabel = new JLabel("Table: " + currentTableName);
            tableLabel.setFont(new Font("Arial", Font.BOLD, 14));
            queryAndSearchPanel.add(tableLabel);
            queryAndSearchPanel.add(Box.createHorizontalStrut(10));

            filterText = new JTextField(12);
            JButton searchBtn = new JButton("Search");
            JButton resetBtn = new JButton("Reset");
            sortColumn = new JComboBox<>();
            sortOrder = new JComboBox<>(new String[]{"None", "Ascending", "Descending"});
            JButton applySort = new JButton("Sort");

            updateSortOptions();

            queryAndSearchPanel.add(new JLabel("Search:"));
            queryAndSearchPanel.add(filterText);
            queryAndSearchPanel.add(searchBtn);
            queryAndSearchPanel.add(resetBtn);
            queryAndSearchPanel.add(Box.createHorizontalStrut(10));
            queryAndSearchPanel.add(new JLabel("Sort:"));
            queryAndSearchPanel.add(sortColumn);
            queryAndSearchPanel.add(sortOrder);
            queryAndSearchPanel.add(applySort);

            controlPanel.add(queryAndSearchPanel, BorderLayout.SOUTH);

            // Add controls and table to the main content panel
            mainContentPanel.add(controlPanel, BorderLayout.NORTH);
            mainContentPanel.add(new JScrollPane(table), BorderLayout.CENTER);

            searchBtn.addActionListener(e -> applyFilterActionFavorites());
            resetBtn.addActionListener(e -> resetFavoritesView());
            applySort.addActionListener(e -> applySortActionFavorites());

            // Defer hiding technical columns until after initial layout
            SwingUtilities.invokeLater(() -> {
                hideColumnByName("fav_id");
                hideColumnByName("user_mobile");
                hideColumnByName("fav_user_mobile");
                updateSortOptions(); // Update sort options after hiding
            });

        } else {
            // --- GENERIC VIEW ---
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            filterColumn = new JComboBox<>();
            sortColumn = new JComboBox<>();
            for (int i = 0; i < model.getColumnCount(); i++) {
                String name = model.getColumnName(i);
                filterColumn.addItem(name);
                sortColumn.addItem(name);
            }
            filterText = new JTextField(14);
            JButton applyFilter = new JButton("Search");
            JButton reset = new JButton("Reset");
            sortOrder = new JComboBox<>(new String[]{"None", "Ascending", "Descending"});
            JButton applySort = new JButton("Sort");

            topPanel.add(new JLabel("Search Column:"));
            topPanel.add(filterColumn);
            topPanel.add(filterText);
            topPanel.add(applyFilter);
            topPanel.add(reset);
            topPanel.add(new JLabel("Sort:"));
            topPanel.add(sortColumn);
            topPanel.add(sortOrder);
            topPanel.add(applySort);

            // Add controls and table to the main content panel
            mainContentPanel.add(topPanel, BorderLayout.NORTH);
            mainContentPanel.add(new JScrollPane(
                    table,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            ), BorderLayout.CENTER);

            applyFilter.addActionListener(e -> applyFilterActionGeneric());
            reset.addActionListener(e -> {
                filterText.setText("");
                sorter.setRowFilter(null);
                sortOrder.setSelectedIndex(0);
                sorter.setSortKeys(null);
            });
            applySort.addActionListener(e -> applySortActionGeneric());

            SwingUtilities.invokeLater(() -> resizeColumnsToFitContent(table));
        }

        // Add the main content panel (controls+table) to the frame's center
        add(mainContentPanel, BorderLayout.CENTER);
    }

    // --- All other methods (fetchTableData, removeSelectedRow, helpers, etc.) remain unchanged ---
    // fetchTableData remains the same
    private DefaultTableModel fetchTableData(String tableName, String userMobile) {
        String sql;
        boolean isFav = tableName.startsWith("Favorites_") && userMobile != null && !userMobile.isEmpty();

        if (isFav) {
            sql = "SELECT * FROM " + tableName + " WHERE user_mobile = ?";
        } else {
            sql = "SELECT * FROM " + tableName;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            if (isFav) {
                pst.setString(1, userMobile);
            }

            try (ResultSet rs = pst.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                Vector<String> columnNames = new Vector<>();
                for (int i = 1; i <= colCount; i++) {
                    columnNames.add(meta.getColumnName(i)); // Use original SQL names for model
                }

                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }
                return new DefaultTableModel(data, columnNames);
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1146) {
                // Error will be shown by the constructor
            } else {
                JOptionPane.showMessageDialog(null, "Error loading table data: " + e.getMessage()); // Use null parent initially
                e.printStackTrace();
            }
            return null; // Return null on error
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // removeSelectedRowFromFavorites remains the same
    private void removeSelectedRowFromFavorites() {
        if (!isFavoritesView) return;

        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to remove.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedViewRow);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        int idColIndex = findModelColumnIndex("fav_id"); // Use helper

        if (idColIndex == -1) {
            JOptionPane.showMessageDialog(this, "❌ Cannot remove: 'fav_id' column not found.", "Deletion Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int favoriteId;
        try {
            favoriteId = (int) model.getValueAt(modelRow, idColIndex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading 'fav_id'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this item?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM " + this.currentTableName + " WHERE fav_id = ? AND user_mobile = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, favoriteId);
            pst.setString(2, this.userMobile);

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "✅ Removed from Favorites!");
                refreshFavoritesTable();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Error removing item: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // refreshFavoritesTable remains the same
    private void refreshFavoritesTable() {
        if (!isFavoritesView) return;
        DefaultTableModel refreshedModel = fetchTableData(currentTableName, userMobile);
        if(refreshedModel != null){
            table.setModel(refreshedModel);
            sorter.setModel(refreshedModel);

            allTableColumns.clear();
            TableColumnModel columnModel = table.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                allTableColumns.add(columnModel.getColumn(i));
            }
            applyNumericComparators(refreshedModel);
            reapplyColumnVisibility(); // Re-hide columns and check checkboxes
            updateSortOptions();
        }
    }


    // --- METHODS FOR FAVORITES VIEW UI ---

    // applyFilterActionFavorites remains the same
    private void applyFilterActionFavorites() {
        String text = filterText.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    // resetFavoritesView remains the same
    private void resetFavoritesView() {
        filterText.setText("");
        sorter.setRowFilter(null);
        if(sortOrder != null) sortOrder.setSelectedIndex(0);
        sorter.setSortKeys(null);
        refreshFavoritesTable(); // Reload data and reset UI state
    }

    // applySortActionFavorites remains the same
    private void applySortActionFavorites() {
        if (sortColumn.getSelectedIndex() < 0 || sortColumn.getItemCount() == 0) return;

        String selectedColName = (String) sortColumn.getSelectedItem();
        int modelIndex = findModelColumnIndex(selectedColName);

        if(modelIndex == -1) return;

        String order = (String) sortOrder.getSelectedItem();
        if (order == null || "None".equals(order)) {
            sorter.setSortKeys(null);
            return;
        }

        SortOrder so = "Ascending".equals(order) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(modelIndex, so)));
    }

    // toggleColumn remains the same
    private void toggleColumn(int modelColIndex) {
        if (!isFavoritesView || columnChecks == null || modelColIndex < 0 || modelColIndex >= columnChecks.length || columnChecks[modelColIndex] == null) return;

        boolean visible = columnChecks[modelColIndex].isSelected();
        TableColumnModel colModel = table.getColumnModel();

        if (visible) {
            TableColumn columnToAdd = null;
            for(TableColumn tc : allTableColumns){
                if(tc.getModelIndex() == modelColIndex){ columnToAdd = tc; break; }
            }
            if(columnToAdd != null){
                int visibleColsBefore = 0;
                for(int i = 0; i < modelColIndex; i++){
                    if(findViewColumnIndex(i) != -1){ visibleColsBefore++; } // Check if currently visible
                }
                colModel.addColumn(columnToAdd);
                if (visibleColsBefore < colModel.getColumnCount() -1) {
                    colModel.moveColumn(colModel.getColumnCount() - 1, visibleColsBefore);
                }
            }
        } else {
            int viewIndex = findViewColumnIndex(modelColIndex);
            if(viewIndex != -1){ colModel.removeColumn(colModel.getColumn(viewIndex)); }
        }
        updateSortOptions();
    }

    // hideColumn modified to use helper
    private void hideColumn(int modelColIndex) {
        int viewIndex = findViewColumnIndex(modelColIndex);
        if(viewIndex != -1){
            TableColumn col = table.getColumnModel().getColumn(viewIndex);
            table.getColumnModel().removeColumn(col);
        }
        // Ensure checkbox exists before trying to access it
        if (columnChecks != null && modelColIndex >= 0 && modelColIndex < columnChecks.length && columnChecks[modelColIndex] != null) {
            columnChecks[modelColIndex].setVisible(false); // Hide the checkbox too
            columnChecks[modelColIndex].setSelected(false); // Ensure it's treated as hidden
        }
    }
    // hideColumnByName helper added
    private void hideColumnByName(String name) {
        int modelIndex = findModelColumnIndex(name);
        if (modelIndex != -1) {
            hideColumn(modelIndex);
        }
    }


    // reapplyColumnVisibility remains the same
    private void reapplyColumnVisibility() {
        if (!isFavoritesView || columnChecks == null || allTableColumns == null) return;

        TableColumnModel tcm = table.getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }

        for (int modelIndex = 0; modelIndex < columnChecks.length; modelIndex++) {
            TableColumn originalCol = null;
            for(TableColumn tc : allTableColumns) {
                if (tc.getModelIndex() == modelIndex) {
                    originalCol = tc;
                    break;
                }
            }

            if (originalCol != null && columnChecks[modelIndex] != null) {
                String colName = table.getModel().getColumnName(modelIndex);
                boolean shouldBeVisible = columnChecks[modelIndex].isSelected();
                boolean isTechnicalCol = colName.equalsIgnoreCase("fav_id") || colName.equalsIgnoreCase("user_mobile") || colName.equalsIgnoreCase("fav_user_mobile");

                if (shouldBeVisible && !isTechnicalCol) {
                    tcm.addColumn(originalCol);
                } else { // Handle both technical cols and user-hidden cols
                    columnChecks[modelIndex].setSelected(false); // Ensure checkbox reflects hidden state
                    if (isTechnicalCol) {
                        columnChecks[modelIndex].setVisible(false); // Also hide checkbox for technical cols
                    }
                }
            }
        }
        // Move columns to approximate original order (simple approach)
        int currentViewIndex = 0;
        for (int modelIndex = 0; modelIndex < columnChecks.length; modelIndex++) {
            if (findViewColumnIndex(modelIndex) != -1) { // If it was added (i.e., visible)
                int viewIndex = findViewColumnIndex(modelIndex);
                if(viewIndex != currentViewIndex){
                    tcm.moveColumn(viewIndex, currentViewIndex);
                }
                currentViewIndex++;
            }
        }
    }

    // updateSortOptions remains the same
    private void updateSortOptions() {
        if (!isFavoritesView || sortColumn == null || table == null) return;
        sortColumn.removeAllItems();
        TableColumnModel colModel = table.getColumnModel();
        TableModel model = table.getModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            String colName = model.getColumnName(colModel.getColumn(i).getModelIndex());
            // Exclude technical columns from sort dropdown
            if (!colName.equalsIgnoreCase("fav_id") && !colName.equalsIgnoreCase("user_mobile") && !colName.equalsIgnoreCase("fav_user_mobile")){
                sortColumn.addItem(colName);
            }
        }
        if (sortColumn.getItemCount() == 0) sortColumn.addItem("None");
    }

    // applyNumericComparators remains the same
    private void applyNumericComparators(DefaultTableModel model) {
        if (sorter == null) return;
        for (int i = 0; i < model.getColumnCount(); i++) {
            final int colIndex = i;
            sorter.setComparator(colIndex, (o1, o2) -> {
                String s1 = o1 == null ? "" : o1.toString();
                String s2 = o2 == null ? "" : o2.toString();
                try {
                    double n1 = Double.parseDouble(s1);
                    double n2 = Double.parseDouble(s2);
                    return Double.compare(n1, n2);
                } catch (NumberFormatException ex) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        }
    }


    // --- METHODS FOR GENERIC VIEW UI ---

    // applyFilterActionGeneric remains the same
    private void applyFilterActionGeneric() {
        String text = filterText.getText().trim();
        int colIndex = Math.max(0, filterColumn.getSelectedIndex());

        if (text.isEmpty()) {
            sorter.setRowFilter(null); return;
        }
        String pattern = "(?i)" + Pattern.quote(text);
        try {
            sorter.setRowFilter(RowFilter.regexFilter(pattern, colIndex));
        } catch (IndexOutOfBoundsException e){
            sorter.setRowFilter(RowFilter.regexFilter(pattern));
        }
    }

    // applySortActionGeneric remains the same
    private void applySortActionGeneric() {
        int colIndex = Math.max(0, sortColumn.getSelectedIndex());
        String order = (String) sortOrder.getSelectedItem();
        if (order == null || "None".equals(order)) {
            sorter.setSortKeys(null); return;
        }

        sorter.setComparator(colIndex, (o1, o2) -> {
            String s1 = o1 == null ? "" : o1.toString();
            String s2 = o2 == null ? "" : o2.toString();
            try {
                double n1 = Double.parseDouble(s1);
                double n2 = Double.parseDouble(s2);
                return Double.compare(n1, n2);
            } catch (NumberFormatException ex) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        SortOrder so = "Ascending".equals(order) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(colIndex, so)));
    }

    // resizeColumnsToFitContent remains the same
    private void resizeColumnsToFitContent(JTable tbl) {
        if(isFavoritesView) return;

        TableColumnModel colModel = tbl.getColumnModel();
        JTableHeader header = tbl.getTableHeader();

        for (int col = 0; col < tbl.getColumnCount(); col++) {
            TableColumn column = colModel.getColumn(col);
            int maxWidth = 100;

            TableCellRenderer hdrRenderer = column.getHeaderRenderer();
            if (hdrRenderer == null) hdrRenderer = header.getDefaultRenderer();
            Component hdrComp = hdrRenderer.getTableCellRendererComponent(tbl, column.getHeaderValue(), false, false, 0, col);
            maxWidth = Math.max(maxWidth, hdrComp.getPreferredSize().width + 18);

            int rowsToCheck = Math.min(tbl.getRowCount(), 100);
            for (int row = 0; row < rowsToCheck; row++) {
                TableCellRenderer cellRenderer = tbl.getCellRenderer(row, col);
                Component c = tbl.prepareRenderer(cellRenderer, row, col);
                maxWidth = Math.max(maxWidth, c.getPreferredSize().width + 18);
            }
            column.setPreferredWidth(maxWidth);
        }
    }

    // --- Utility Helper Methods ---

    /** Finds the current view index of a column given its model index. Returns -1 if not visible. */
    private int findViewColumnIndex(int modelIndex) {
        if (table == null) return -1;
        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelIndex) {
                return i;
            }
        }
        return -1;
    }

    /** Finds the model index of a column given its name (case-insensitive). Returns -1 if not found. */
    private int findModelColumnIndex(String columnName) {
        if (table == null || columnName == null) return -1;
        TableModel model = table.getModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }
}
