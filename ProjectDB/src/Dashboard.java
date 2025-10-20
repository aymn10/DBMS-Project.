import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Dashboard extends JFrame {
    JPanel topButtonContainer;
    JPanel contentPanel, tableBtnPanel;
    JTable table;
    TableRowSorter<TableModel> sorter;
    JComboBox<String> sortColumn, sortOrder;
    JCheckBox[] columnChecks;
    String currentTable = "";

    private java.util.List<TableColumn> allTableColumns;
    private String fname;
    private String lname;
    private JLabel welcomeLabel;
    private String userMobile;

    public Dashboard(String fname, String lname, String mobile) {
        this.fname = fname;
        this.lname = lname;
        this.userMobile = mobile;

        setTitle("Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        welcomeLabel = new JLabel("🌟 Welcome, " + fname + " " + lname + " 🌟", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 22));
        add(welcomeLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        add(centerPanel, BorderLayout.CENTER);

        topButtonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        centerPanel.add(topButtonContainer);

        contentPanel = new JPanel(new BorderLayout());
        centerPanel.add(contentPanel);

        tableBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        add(tableBtnPanel, BorderLayout.SOUTH);

        showMainButtons();
    }

    public void showMainButtons() {
        topButtonContainer.removeAll();
        contentPanel.removeAll();
        tableBtnPanel.setVisible(false);

        JButton viewCollegeBtn = createCuteButton("View Colleges", new Color(135, 206, 250));
        JButton predictCollegeBtn = createCuteButton("Predict College", new Color(255, 182, 193));
        JButton logoutBtn = createCuteButton("Logout", new Color(240, 128, 128));

        viewCollegeBtn.addActionListener(e -> showTableButtons());
        predictCollegeBtn.addActionListener(e -> {
            contentPanel.removeAll();
            tableBtnPanel.removeAll();
            tableBtnPanel.setVisible(false);
            tableBtnPanel.revalidate();
            tableBtnPanel.repaint();

            CollegePredictor predictorPanel = new CollegePredictor(fname, lname, this, userMobile);
            contentPanel.add(predictorPanel, BorderLayout.CENTER);

            contentPanel.revalidate();
            contentPanel.repaint();
        });
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginPage().setVisible(true);
        });

        topButtonContainer.add(viewCollegeBtn);
        topButtonContainer.add(predictCollegeBtn);
        topButtonContainer.add(logoutBtn);

        topButtonContainer.revalidate();
        topButtonContainer.repaint();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void showPredictorResultButtons(JPanel predictorButtons) {
        topButtonContainer.removeAll();
        topButtonContainer.add(predictorButtons);
        topButtonContainer.revalidate();
        topButtonContainer.repaint();
    }

    private void showTableButtons() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();

        tableBtnPanel.removeAll();
        tableBtnPanel.setVisible(true);

        JButton btnCollegeInfo = createCuteButton("College Info", new Color(135, 206, 250));
        JButton btnBranchFees = createCuteButton("Branch Fees", new Color(255, 182, 193));
        JButton btnCETCutoff = createCuteButton("Cutoff", new Color(240, 128, 128));
        JButton btnJeeCutoff = createCuteButton("JEE Cutoff", new Color(144, 238, 144));
        JButton btnCETPercentileCutoff = createCuteButton("Percentile Cutoff", new Color(255, 215, 0));

        tableBtnPanel.add(btnCollegeInfo);
        tableBtnPanel.add(btnBranchFees);
        tableBtnPanel.add(btnCETCutoff);
        tableBtnPanel.add(btnJeeCutoff);
        tableBtnPanel.add(btnCETPercentileCutoff);

        btnCollegeInfo.addActionListener(e -> loadTable("CollegeInfo"));
        btnBranchFees.addActionListener(e -> loadTable("BranchFees"));
        btnCETCutoff.addActionListener(e -> loadTable("cet_rankcutoff"));
        btnJeeCutoff.addActionListener(e -> loadTable("jeeCutoff"));
        btnCETPercentileCutoff.addActionListener(e -> loadTable("PercentileCutoff"));

        JButton btnPlacement = createCuteButton("Placement Stats", new Color(32, 178, 170));
        JButton btnRecruiters = createCuteButton("Top Recruiters", new Color(218, 112, 214));
        JButton btnSocial = createCuteButton("Social Media", new Color(60, 179, 113));
        JButton btnRating = createCuteButton("User Rating", new Color(255, 165, 0));

        tableBtnPanel.add(btnPlacement);
        tableBtnPanel.add(btnRecruiters);
        tableBtnPanel.add(btnSocial);
        tableBtnPanel.add(btnRating);

        btnPlacement.addActionListener(e -> loadTable("PlacementStats"));
        btnRecruiters.addActionListener(e -> loadTable("TopRecruiters"));
        btnSocial.addActionListener(e -> loadTable("SocialMediaHandles"));
        btnRating.addActionListener(e -> loadTable("UserRatings"));

        tableBtnPanel.revalidate();
        tableBtnPanel.repaint();
    }

    private JButton createCuteButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // --- MODIFIED METHOD ---
    private void loadTable(String tableName) {
        currentTable = tableName;
        contentPanel.removeAll();
        contentPanel.setLayout(new BorderLayout());

        DefaultTableModel model = fetchTableData(tableName);
        if (model == null || model.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "Could not load data for '" + tableName + "'. It may not exist yet.", "Load Error", JOptionPane.WARNING_MESSAGE);
            showMainButtons();
            return;
        }
        table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addToFavoritesItem = new JMenuItem("⭐ Add to Favorites");
        popupMenu.add(addToFavoritesItem);
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

        addToFavoritesItem.addActionListener(e -> addSelectedRowToFavorites());

        allTableColumns = new ArrayList<>();
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            allTableColumns.add(columnModel.getColumn(i));
        }

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        applyNumericComparators(model);

        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(220, 220, 220));
        table.getTableHeader().setReorderingAllowed(false);

        resizeColumnWidths(table);

        JPanel controlPanel = new JPanel(new BorderLayout());

        // --- NEW FAVORITES BUTTON (Repositioned) ---
        JButton favoritesBtn = createCuteButton("My Favorites", new Color(255, 105, 180));
        favoritesBtn.addActionListener(e -> viewFavorites(currentTable));

        // --- COLUMN CHECKBOX PANEL ---
        JPanel columnCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        columnChecks = new JCheckBox[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            final int colIndex = i;
            columnChecks[i] = new JCheckBox(model.getColumnName(i), true);
            columnChecks[i].addActionListener(e -> toggleColumn(colIndex));
            columnCheckPanel.add(columnChecks[i]);
        }
        JScrollPane checkScroll = new JScrollPane(columnCheckPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        checkScroll.setPreferredSize(new Dimension(800, 45));

        // --- NEW NORTH PANEL to hold Checkboxes and Favorites Button ---
        JPanel northControlPanel = new JPanel(new BorderLayout(10, 0));
        northControlPanel.add(checkScroll, BorderLayout.CENTER);
        northControlPanel.add(favoritesBtn, BorderLayout.EAST); // Button in top-right

        controlPanel.add(northControlPanel, BorderLayout.NORTH); // Add new combined panel to NORTH
        // --- END OF REPOSITIONING ---

        JPanel queryAndSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel tableLabel = new JLabel("Table: " + tableName);
        tableLabel.setFont(new Font("Arial", Font.BOLD, 14));
        queryAndSearchPanel.add(tableLabel);

        if (tableName.equals("BranchFees")) {
            JButton avgBtn = createCuteButton("Show Avg Fees", new Color(70, 130, 180));
            JTextField minFee = new JTextField(6);
            JTextField maxFee = new JTextField(6);
            JButton rangeBtn = createCuteButton("Apply Range", new Color(70, 130, 180));
            avgBtn.addActionListener(e -> runQuery("SELECT CourseName, AVG(Fees) AS AvgFee FROM BranchFees GROUP BY CourseName"));
            rangeBtn.addActionListener(e -> {
                try {
                    int min = Integer.parseInt(minFee.getText().trim());
                    int max = Integer.parseInt(maxFee.getText().trim());
                    runQuery("SELECT * FROM BranchFees WHERE Fees BETWEEN " + min + " AND " + max);
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Enter valid min & max fee."); }
            });
            queryAndSearchPanel.add(avgBtn);
            queryAndSearchPanel.add(new JLabel("Min Fee:")); queryAndSearchPanel.add(minFee);
            queryAndSearchPanel.add(new JLabel("Max Fee:")); queryAndSearchPanel.add(maxFee);
            queryAndSearchPanel.add(rangeBtn);
        }

        if (tableName.equals("CollegeInfo")) {
            JButton countBtn = createCuteButton("Show University Count", new Color(70, 130, 180));
            countBtn.addActionListener(e -> runQuery("SELECT University, COUNT(*) AS CollegeCount FROM CollegeInfo GROUP BY University"));
            queryAndSearchPanel.add(countBtn);
        }

        JTextField searchText = new JTextField(12);
        JButton searchBtn = new JButton("Search");
        JButton resetBtn = new JButton("Reset");
        JButton downloadBtn = createCuteButton("Download CSV", new Color(34, 139, 34));
        sortColumn = new JComboBox<>();
        sortOrder = new JComboBox<>(new String[]{"None", "Ascending", "Descending"});
        JButton applySort = new JButton("Sort");

        updateSortOptions(sortColumn);

        queryAndSearchPanel.add(new JLabel("Search:"));
        queryAndSearchPanel.add(searchText);
        queryAndSearchPanel.add(searchBtn);
        queryAndSearchPanel.add(resetBtn);
        queryAndSearchPanel.add(downloadBtn);
        queryAndSearchPanel.add(new JLabel("Sort:"));
        queryAndSearchPanel.add(sortColumn);
        queryAndSearchPanel.add(sortOrder);
        queryAndSearchPanel.add(applySort);
        // Removed favoritesBtn from here

        controlPanel.add(queryAndSearchPanel, BorderLayout.SOUTH);

        contentPanel.add(controlPanel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String text = searchText.getText().trim();
            if (text.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        });
        resetBtn.addActionListener(e -> loadTable(currentTable));
        applySort.addActionListener(e -> applySortAction());
        downloadBtn.addActionListener(e -> CsvExporter.exportToCsv(table, Dashboard.this));

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void viewFavorites(String originalTableName) {
        String favTableName = "Favorites_" + originalTableName;
        TableViewer favoritesViewer = new TableViewer(favTableName, this.userMobile);
        favoritesViewer.setVisible(true);
    }

    private void addSelectedRowToFavorites() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to add.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String favTableName = "Favorites_" + this.currentTable;
        int modelRow = table.convertRowIndexToModel(selectedViewRow);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        StringBuilder sql = new StringBuilder("INSERT INTO " + favTableName + " (user_mobile, ");
        StringBuilder values = new StringBuilder(" VALUES (?, ");
        ArrayList<Object> params = new ArrayList<>();
        params.add(this.userMobile);

        for (int i = 0; i < model.getColumnCount(); i++) {
            sql.append("`").append(model.getColumnName(i)).append("`");
            values.append("?");
            params.add(model.getValueAt(modelRow, i));

            if (i < model.getColumnCount() - 1) {
                sql.append(", ");
                values.append(", ");
            }
        }

        sql.append(")");
        values.append(")");
        String finalSql = sql.toString() + values.toString();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(finalSql)) {

            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Added to Favorites!");

        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "This item is already in your favorites.", "Duplicate", JOptionPane.INFORMATION_MESSAGE);
            } else if (ex.getErrorCode() == 1146) {
                JOptionPane.showMessageDialog(this, "❌ Error: Table '" + favTableName + "' does not exist.\nPlease ask the administrator to create it.", "Table Not Found", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Error adding to favorites: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void runQuery(String sql) {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            Vector<String> columnNames = new Vector<>();
            for (int i = 1; i <= colCount; i++) {
                columnNames.add(meta.getColumnName(i));
            }
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }

            DefaultTableModel newModel = new DefaultTableModel(data, columnNames);
            table.setModel(newModel);
            sorter = new TableRowSorter<>(newModel);
            table.setRowSorter(sorter);
            applyNumericComparators(newModel);
            updateSortOptions(sortColumn);
            resizeColumnWidths(table);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error running query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleColumn(int colIndex) {
        TableColumnModel colModel = table.getColumnModel();
        while (colModel.getColumnCount() > 0) {
            colModel.removeColumn(colModel.getColumn(0));
        }
        for (int i = 0; i < allTableColumns.size(); i++) {
            if (columnChecks[i].isSelected()) {
                colModel.addColumn(allTableColumns.get(i));
            }
        }
        updateSortOptions(sortColumn);
    }

    private void updateSortOptions(JComboBox<String> sortColumn) {
        sortColumn.removeAllItems();
        TableColumnModel colModel = table.getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            String colName = colModel.getColumn(i).getHeaderValue().toString();
            sortColumn.addItem(colName);
        }
        if (sortColumn.getItemCount() == 0) sortColumn.addItem("None");
    }

    private void applySortAction() {
        if (sortColumn.getSelectedIndex() < 0) return;
        int colIndexToView = sortColumn.getSelectedIndex();
        TableColumn selectedColumn = table.getColumnModel().getColumn(colIndexToView);
        int modelIndex = selectedColumn.getModelIndex();
        String order = (String) sortOrder.getSelectedItem();
        if ("None".equals(order)) {
            sorter.setSortKeys(null);
            return;
        }
        sorter.setSortKeys(List.of(
                new RowSorter.SortKey(modelIndex, order.equals("Ascending") ? SortOrder.ASCENDING : SortOrder.DESCENDING)
        ));
    }

    private DefaultTableModel fetchTableData(String tableName) {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            Vector<String> columnNames = new Vector<>();
            for (int i = 1; i <= colCount; i++) columnNames.add(meta.getColumnName(i));
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= colCount; i++) row.add(rs.getObject(i));
                data.add(row);
            }
            return new DefaultTableModel(data, columnNames);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1146) {
                JOptionPane.showMessageDialog(this, "Error: Table '" + tableName + "' does not exist.", "Table Not Found", JOptionPane.ERROR_MESSAGE);
            } else {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void applyNumericComparators(DefaultTableModel model) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            final int colIndex = i;
            sorter.setComparator(colIndex, (o1, o2) -> {
                try {
                    if (o1 == null || o2 == null) return 0;
                    double d1 = Double.parseDouble(o1.toString());
                    double d2 = Double.parseDouble(o2.toString());
                    return Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
        }
    }

    private void resizeColumnWidths(JTable table) {
        TableColumnModel columnModel = table.getColumnModel();
        for (int col = 0; col < table.getColumnCount(); col++) {
            int maxWidth = 75;
            TableColumn column = columnModel.getColumn(col);
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, 0, col);
            maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
                Component c = table.prepareRenderer(cellRenderer, row, col);
                maxWidth = Math.max(maxWidth, c.getPreferredSize().width + 10);
            }
            column.setPreferredWidth(maxWidth);
        }
    }

    public void updateUserName(String fname, String lname) {
        this.fname = fname;
        this.lname = lname;
        welcomeLabel.setText("🌟 Welcome, " + fname + " " + lname + " 🌟");
    }
}
