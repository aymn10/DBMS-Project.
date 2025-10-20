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
import java.util.regex.Pattern; // Needed for reset filter

public class CollegePredictor extends JPanel {

    private JPanel formPanel;
    private JPanel resultPanel;
    private Dashboard dashboard;
    private String userMobile;
    private String currentResultType = ""; // To track which result is shown for Fav table

    private JTable resultTable; // Use this for both results and recommendations
    private TableRowSorter<TableModel> sorter;
    private JComboBox<String> sortColumn;
    private JComboBox<String> sortOrder;
    private JCheckBox[] columnChecks;
    private java.util.List<TableColumn> allTableColumns;

    public CollegePredictor(String fname, String lname, Dashboard dashboard, String userMobile) {
        this.dashboard = dashboard;
        this.userMobile = userMobile;
        setLayout(new BorderLayout(10, 10));

        createFormPanel(fname, lname);
        add(formPanel, BorderLayout.NORTH);

        resultPanel = new JPanel(new BorderLayout());
        add(resultPanel, BorderLayout.CENTER);
    }

    // createStudentInfoPanel remains the same
    private JPanel createStudentInfoPanel(String name, String gender, String cetRank, String cetPercentile, String jeeRank, String jeePercentile) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Your Submitted Data"));

        panel.add(new JLabel("<html><b>Name:</b> " + (name.isEmpty() ? "N/A" : name) + "</html>"));
        panel.add(new JLabel("<html><b>Gender:</b> " + (gender.isEmpty() ? "N/A" : gender) + "</html>"));
        panel.add(new JLabel("<html><b>CET Rank:</b> " + (cetRank.isEmpty() ? "N/A" : cetRank) + "</html>"));
        panel.add(new JLabel("<html><b>CET Percentile:</b> " + (cetPercentile.isEmpty() ? "N/A" : cetPercentile) + "</html>"));
        panel.add(new JLabel("<html><b>JEE Rank:</b> " + (jeeRank.isEmpty() ? "N/A" : jeeRank) + "</html>"));
        panel.add(new JLabel("<html><b>JEE Percentile:</b> " + (jeePercentile.isEmpty() ? "N/A" : jeePercentile) + "</html>"));
        return panel;
    }

    // submitPredictionData remains the same
    private void submitPredictionData(String name, String gender, String cetRank, String cetPercentile, String jeeRank, String jeePercentile) {
        String sql = "INSERT INTO UserPredictions (name, gender, cet_rank, cet_percentile, jee_rank, jee_percentile) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, gender);
            pst.setObject(3, cetRank.isEmpty() ? null : Integer.parseInt(cetRank));
            pst.setObject(4, cetPercentile.isEmpty() ? null : Double.parseDouble(cetPercentile));
            pst.setObject(5, jeeRank.isEmpty() ? null : Integer.parseInt(jeeRank));
            pst.setObject(6, jeePercentile.isEmpty() ? null : Double.parseDouble(jeePercentile));

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Your information has been saved! Now showing prediction options.");

            this.removeAll();
            JPanel studentInfoPanel = createStudentInfoPanel(name, gender, cetRank, cetPercentile, jeeRank, jeePercentile);
            this.add(studentInfoPanel, BorderLayout.NORTH);
            this.add(resultPanel, BorderLayout.CENTER);
            this.revalidate();
            this.repaint();

            showResultButtons(cetPercentile, jeePercentile, cetRank, gender);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "❌ Please enter valid numbers for ranks and percentiles.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Error saving data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Displays Top 10 Recommendations view
    // CORRECTED: Passes correct resultType ("Top10Recommendations")
    private void displayRecommendations(String cetPercentile, String cetRank, String jeePercentile) {
        String sql;
        String score;
        final String resultType = "Top10Recommendations"; // Specific type for this view
        this.currentResultType = resultType; // Store current view type

        // SQL queries remain the same
        if (cetPercentile != null && !cetPercentile.isEmpty()) {
            sql = "SELECT bf.ChoiceCode, ci.InstituteCode, ci.InstituteName, bf.CourseName, ci.Location FROM PercentileCutoff pc JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE pc.CAP1_Percentile <= ? ORDER BY pc.CAP1_Percentile DESC LIMIT 10";
            score = cetPercentile;
        } else if (cetRank != null && !cetRank.isEmpty()) {
            sql = "SELECT bf.ChoiceCode, ci.InstituteCode, ci.InstituteName, bf.CourseName, ci.Location FROM cet_rankcutoff c JOIN BranchFees bf ON c.Choice_Code = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE c.CAP1_Rank >= ? ORDER BY c.CAP1_Rank ASC LIMIT 10";
            score = cetRank;
        } else if (jeePercentile != null && !jeePercentile.isEmpty()) {
            sql = "SELECT bf.ChoiceCode, ci.InstituteCode, ci.InstituteName, bf.CourseName, ci.Location FROM jeeCutoff jc JOIN BranchFees bf ON jc.ChoiceCode = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE jc.JEE_Percentile <= ? ORDER BY jc.JEE_Percentile DESC LIMIT 10";
            score = jeePercentile;
        } else {
            JOptionPane.showMessageDialog(this, "Please provide a score for recommendations.", "Input Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, Double.parseDouble(score));
            ResultSet rs = pst.executeQuery();
            DefaultTableModel tableModel = buildTableModel(rs);

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No recommendations found.", "No Results", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            resultTable = new JTable(tableModel);
            setupBasicTableView(resultTable);

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem addToFavoritesItem = new JMenuItem("⭐ Add to Favorites");
            popupMenu.add(addToFavoritesItem);
            resultTable.setComponentPopupMenu(popupMenu);
            resultTable.addMouseListener(createRightClickListener());
            // Pass the specific resultType ("Top10Recommendations")
            addToFavoritesItem.addActionListener(e -> addSelectedRowToFavorites(resultTable, resultType));

            resultPanel.removeAll();
            JPanel recommendationPanel = new JPanel(new BorderLayout(5, 5));
            // Pass the specific resultType
            JPanel northPanel = createTopButtonPanel(resultType);
            recommendationPanel.add(northPanel, BorderLayout.NORTH);
            recommendationPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

            resultPanel.add(recommendationPanel, BorderLayout.CENTER);
            resultPanel.revalidate();
            resultPanel.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching recommendations: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Displays Detailed Results view (CET/JEE Rank/Percentile)
    // CORRECTED: Passes correct resultType determined by 'type' parameter
    private void displayResults(String score, String type, String gender) {
        if (score == null || score.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No score entered for this category.", "Missing Score", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "";
        final String resultType; // Determined by 'type'
        boolean isFemale = "Female".equalsIgnoreCase(gender);

        switch (type) {
            case "CET_PERCENTILE":
                resultType = "CETPercentileResults";
                sql = isFemale ? /* Female Query */
                        "SELECT bf.ChoiceCode, ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, pc.CAP1_Percentile, pc.CAP2_Percentile, pc.CAP3_Percentile, pc.CAP_LOPEN1_Percentile, pc.CAP_LOPEN2_Percentile, pc.CAP_LOPEN3_Percentile FROM PercentileCutoff pc JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE LEAST(IFNULL(pc.CAP1_Percentile, 101), IFNULL(pc.CAP2_Percentile, 101), IFNULL(pc.CAP3_Percentile, 101), IFNULL(pc.CAP_LOPEN1_Percentile, 101), IFNULL(pc.CAP_LOPEN2_Percentile, 101), IFNULL(pc.CAP_LOPEN3_Percentile, 101)) <= ? ORDER BY ci.InstituteName, bf.CourseName"
                        : /* Male/Other Query */
                        "SELECT bf.ChoiceCode, ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, pc.CAP1_Percentile, pc.CAP2_Percentile, pc.CAP3_Percentile FROM PercentileCutoff pc JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE LEAST(IFNULL(pc.CAP1_Percentile, 101), IFNULL(pc.CAP2_Percentile, 101), IFNULL(pc.CAP3_Percentile, 101)) <= ? ORDER BY ci.InstituteName, bf.CourseName";
                break;
            case "JEE_PERCENTILE":
                resultType = "JEEPercentileResults";
                sql = "SELECT bf.ChoiceCode, ci.InstituteName, bf.CourseName, ci.Location, bf.Fees, jc.JEE_Percentile, jc.JEE_Rank FROM jeeCutoff jc JOIN BranchFees bf ON jc.ChoiceCode = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE jc.JEE_Percentile <= ? ORDER BY jc.JEE_Percentile DESC";
                break;
            case "CET_RANK":
                resultType = "CETRankResults";
                sql = isFemale ? /* Female Query */
                        "SELECT bf.ChoiceCode, ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, c.CAP1_Rank, c.CAP2_Rank, c.CAP3_Rank, c.CAP_LOPEN1_Rank, c.CAP_LOPEN2_Rank, c.CAP_LOPEN3_Rank FROM cet_rankcutoff c JOIN BranchFees bf ON c.Choice_Code = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE ? <= GREATEST(IFNULL(c.CAP1_Rank, 0), IFNULL(c.CAP2_Rank, 0), IFNULL(c.CAP3_Rank, 0), IFNULL(c.CAP_LOPEN1_Rank, 0), IFNULL(c.CAP_LOPEN2_Rank, 0), IFNULL(c.CAP_LOPEN3_Rank, 0)) ORDER BY ci.InstituteName, bf.CourseName"
                        : /* Male/Other Query */
                        "SELECT bf.ChoiceCode, ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, c.CAP1_Rank, c.CAP2_Rank, c.CAP3_Rank FROM cet_rankcutoff c JOIN BranchFees bf ON c.Choice_Code = bf.ChoiceCode JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode WHERE ? <= GREATEST(IFNULL(c.CAP1_Rank, 0), IFNULL(c.CAP2_Rank, 0), IFNULL(c.CAP3_Rank, 0)) ORDER BY ci.InstituteName, bf.CourseName";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown result type: " + type, "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }
        this.currentResultType = resultType; // Store current view type for label

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, Double.parseDouble(score));
            ResultSet rs = pst.executeQuery();
            DefaultTableModel tableModel = buildTableModel(rs);

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No colleges found matching score.", "No Results", JOptionPane.INFORMATION_MESSAGE);
                resultPanel.removeAll();
                resultPanel.revalidate();
                resultPanel.repaint();
                return;
            }

            resultTable = new JTable(tableModel);
            setupBasicTableView(resultTable);
            // Pass the specific resultType
            setupFullTableView(resultTable, tableModel, resultType);

            resultPanel.removeAll();
            // Pass the specific resultType
            resultPanel.add(createFullControlPanel(resultType), BorderLayout.NORTH);
            resultPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
            resultPanel.revalidate();
            resultPanel.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching prediction results: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --- Helper Methods ---

    /** Applies common styling to the JTable */
    private void setupBasicTableView(JTable tbl) {
        tbl.setRowHeight(25);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tbl.setFillsViewportHeight(true);
        tbl.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tbl.getTableHeader().setBackground(new Color(220, 220, 220));
        tbl.getTableHeader().setReorderingAllowed(false);
    }

    /** Sets up sorter, comparators, column storage, and right-click for the full view */
    // CORRECTED: Takes resultType
    private void setupFullTableView(JTable tbl, DefaultTableModel model, String resultType) {
        sorter = new TableRowSorter<>(model);
        tbl.setRowSorter(sorter);
        applyNumericComparators(model);

        allTableColumns = new ArrayList<>();
        TableColumnModel columnModel = tbl.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            allTableColumns.add(columnModel.getColumn(i));
        }

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addToFavoritesItem = new JMenuItem("⭐ Add to Favorites");
        popupMenu.add(addToFavoritesItem);
        tbl.setComponentPopupMenu(popupMenu);
        tbl.addMouseListener(createRightClickListener());
        // Pass the specific resultType
        addToFavoritesItem.addActionListener(e -> addSelectedRowToFavorites(tbl, resultType));
    }

    /** Creates the top control panel with Download/Favorites for simple view */
    // CORRECTED: Takes resultType
    private JPanel createTopButtonPanel(String resultType) {
        JPanel northPanel = new JPanel(new BorderLayout(10, 0));
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton downloadBtn = createCuteButton("Download CSV", new Color(34, 139, 34));
        JButton favoritesBtn = createCuteButton("My Favorites", new Color(255, 105, 180));

        downloadBtn.addActionListener(e -> CsvExporter.exportToCsv(resultTable, this));
        // Pass the specific resultType
        favoritesBtn.addActionListener(e -> viewFavorites(resultType));

        buttonGroup.add(downloadBtn);
        northPanel.add(buttonGroup, BorderLayout.WEST);
        northPanel.add(favoritesBtn, BorderLayout.EAST);
        return northPanel;
    }

    /** Creates the full control panel (Checkboxes, Search, Sort) for detailed view */
    // CORRECTED: Takes resultType
    private JPanel createFullControlPanel(String resultType) {
        JPanel controlPanel = new JPanel(new BorderLayout());
        TableModel model = resultTable.getModel();

        // Top Part: Column Checkboxes + Favorites Button
        JButton favoritesBtn = createCuteButton("My Favorites", new Color(255, 105, 180));
        // Pass the specific resultType
        favoritesBtn.addActionListener(e -> viewFavorites(resultType));

        JPanel columnCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        columnChecks = new JCheckBox[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            final int colIndex = i;
            columnChecks[i] = new JCheckBox(model.getColumnName(i), true);
            columnChecks[i].addActionListener(e -> toggleColumn(colIndex));
            columnCheckPanel.add(columnChecks[i]);
        }
        JScrollPane checkScroll = new JScrollPane(columnCheckPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        checkScroll.setPreferredSize(new Dimension(800, 45));

        JPanel northControlPanel = new JPanel(new BorderLayout(10, 0));
        northControlPanel.add(checkScroll, BorderLayout.CENTER);
        northControlPanel.add(favoritesBtn, BorderLayout.EAST);
        controlPanel.add(northControlPanel, BorderLayout.NORTH);

        // Bottom Part: Table Label, Search, Sort, Reset, Download
        JPanel searchAndSortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JLabel tableLabel = new JLabel("Table: " + currentResultType); // Display specific result type name
        tableLabel.setFont(new Font("Arial", Font.BOLD, 14));
        searchAndSortPanel.add(tableLabel);
        searchAndSortPanel.add(Box.createHorizontalStrut(10));

        JTextField searchText = new JTextField(12);
        JButton searchBtn = new JButton("Search");
        JButton resetBtn = new JButton("Reset");
        JButton downloadBtn = createCuteButton("Download CSV", new Color(34, 139, 34));
        sortColumn = new JComboBox<>();
        sortOrder = new JComboBox<>(new String[]{"None", "Ascending", "Descending"});
        JButton applySort = new JButton("Sort");

        updateSortOptions();

        searchAndSortPanel.add(new JLabel("Search:"));
        searchAndSortPanel.add(searchText);
        searchAndSortPanel.add(searchBtn);
        searchAndSortPanel.add(resetBtn);
        searchAndSortPanel.add(Box.createHorizontalStrut(10));
        searchAndSortPanel.add(downloadBtn);
        searchAndSortPanel.add(Box.createHorizontalStrut(10));
        searchAndSortPanel.add(new JLabel("Sort:"));
        searchAndSortPanel.add(sortColumn);
        searchAndSortPanel.add(sortOrder);
        searchAndSortPanel.add(applySort);
        controlPanel.add(searchAndSortPanel, BorderLayout.SOUTH);

        // Action Listeners
        searchBtn.addActionListener(e -> {
            String text = searchText.getText().trim();
            if (text.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        });
        resetBtn.addActionListener(e -> {
            searchText.setText("");
            sorter.setRowFilter(null);
            if(sortOrder != null) sortOrder.setSelectedIndex(0);
            sorter.setSortKeys(null);
        });
        downloadBtn.addActionListener(e -> CsvExporter.exportToCsv(resultTable, this));
        applySort.addActionListener(e -> applySortAction());

        return controlPanel;
    }

    /** Creates a MouseAdapter for handling right-clicks on tables */
    private MouseAdapter createRightClickListener() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && e.getSource() instanceof JTable) {
                    JTable sourceTable = (JTable) e.getSource();
                    int row = sourceTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < sourceTable.getRowCount()) {
                        sourceTable.setRowSelectionInterval(row, row);
                    } else {
                        sourceTable.clearSelection();
                    }
                }
            }
        };
    }

    /** Opens the TableViewer for the specific favorites table */
    // CORRECTED: Uses resultType to build table name
    private void viewFavorites(String resultType) {
        if (resultType == null || resultType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot determine which favorites to view.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String favTableName = "Favorites_" + resultType; // Use the specific type
        TableViewer favoritesViewer = new TableViewer(favTableName, this.userMobile);
        favoritesViewer.setVisible(true);
    }

    /** Adds the selected row from the JTable to the specified favorites table */
    // CORRECTED: Uses resultType to build table name and saves displayed columns
    private void addSelectedRowToFavorites(JTable targetTable, String resultType) {
        int selectedViewRow = targetTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to add.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (resultType == null || resultType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot determine target favorites table.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String favTableName = "Favorites_" + resultType; // Use the specific type
        int modelRow = targetTable.convertRowIndexToModel(selectedViewRow);
        TableModel model = targetTable.getModel();

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        ArrayList<Object> values = new ArrayList<>();

        columns.append("user_mobile");
        placeholders.append("?");
        values.add(this.userMobile);

        // Get data directly from the table model (displayed columns)
        for (int i = 0; i < model.getColumnCount(); i++) {
            String displayColumnName = model.getColumnName(i);
            // Convert display name (like "Choice Code", "CAP1 Percentile") back to SQL name
            // Basic conversion: remove space. Adjust if your DB names differ more significantly.
            String sqlColumnName = displayColumnName.replace(" ", "");

            // Handle potential specific naming differences if needed
            // Example: If display is "Choice Code" but DB is "ChoiceCode" (handled by replace)
            // Example: If display is "CAP1 Percentile" but DB is "CAP1_Percentile"
            if (displayColumnName.endsWith(" Percentile")) {
                sqlColumnName = displayColumnName.replace(" Percentile", "_Percentile");
            } else if (displayColumnName.endsWith(" Rank")) {
                sqlColumnName = displayColumnName.replace(" Rank", "_Rank");
            }
            // Add more specific conversions if your display names and DB names differ

            if (sqlColumnName.isEmpty()) continue; // Skip if conversion results in empty name

            columns.append(", `").append(sqlColumnName).append("`");
            placeholders.append(", ?");
            values.add(model.getValueAt(modelRow, i));
        }

        String sql = "INSERT INTO " + favTableName + " (" + columns.toString() + ") VALUES (" + placeholders.toString() + ")";

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                pst.setObject(i + 1, values.get(i));
            }
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Added to Favorites (" + resultType + ")!");
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "Already in favorites.", "Duplicate", JOptionPane.INFORMATION_MESSAGE);
            } else if (ex.getErrorCode() == 1146) {
                JOptionPane.showMessageDialog(this, "❌ Error: Favorites table '" + favTableName + "' not found.", "Table Not Found", JOptionPane.ERROR_MESSAGE);
            } else if (ex.getMessage().toLowerCase().contains("column count doesn't match value count")) {
                JOptionPane.showMessageDialog(this, "❌ Error: Mismatch between columns in view and table '" + favTableName + "'. Check table structure.", "Save Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("SQL: " + sql);
                System.err.println("Values: " + values);
            } else if (ex.getMessage().contains("doesn't have a default value")) {
                JOptionPane.showMessageDialog(this, "❌ Error: A required column in '" + favTableName + "' is missing a value.", "Save Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("SQL: " + sql);
                System.err.println("Values: " + values);
            }
            else {
                JOptionPane.showMessageDialog(this, "❌ Error adding favorite: " + ex.getMessage());
                ex.printStackTrace();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // --- Unchanged Methods Below ---

    public static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            String colName = metaData.getColumnName(column);
            // Use original name for model, convert for display later if needed
            // Handle Choice_Code specifically to store original name if needed later
//             if(colName.equalsIgnoreCase("Choice_Code")) {
//                 columnNames.add("Choice_Code"); // Store original if needed for saving
//             } else {
            columnNames.add(colName); // Store original SQL name
//             }
        }
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }
        // Create model with original SQL names
        DefaultTableModel model = new DefaultTableModel(data, columnNames);

        // --- Optional: Create display names (alternative to doing it in getColumnName override) ---
        Vector<String> displayNames = new Vector<>(columnNames);
        for(int i=0; i< displayNames.size(); i++){
            if(displayNames.get(i).equalsIgnoreCase("Choice_Code")){
                displayNames.set(i, "Choice Code");
            } else {
                displayNames.set(i, displayNames.get(i).replace("_", " "));
            }
        }
        model.setColumnIdentifiers(displayNames); // Set display names after creation
        // --- End Optional ---

        return model;
    }


    private void createFormPanel(String fname, String lname) {
        formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(fname + " " + lname, 15);
        formPanel.add(nameField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 3;
        JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male", "Female"});
        formPanel.add(genderBox, gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        formPanel.add(new JLabel("CET Rank:"), gbc);
        gbc.gridx = 1;
        JTextField cetRankField = new JTextField(15);
        formPanel.add(cetRankField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("CET Percentile:"), gbc);
        gbc.gridx = 3;
        JTextField cetPercentileField = new JTextField(15);
        formPanel.add(cetPercentileField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        formPanel.add(new JLabel("JEE Rank:"), gbc);
        gbc.gridx = 1;
        JTextField jeeRankField = new JTextField(15);
        formPanel.add(jeeRankField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("JEE Percentile:"), gbc);
        gbc.gridx = 3;
        JTextField jeePercentileField = new JTextField(15);
        formPanel.add(jeePercentileField, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton submitBtn = createCuteButton("Submit Info & Predict", new Color(144, 238, 144));
        formPanel.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            submitPredictionData(
                    nameField.getText(),
                    (String) genderBox.getSelectedItem(),
                    cetRankField.getText(),
                    cetPercentileField.getText(),
                    jeeRankField.getText(),
                    jeePercentileField.getText()
            );
        });
    }

    private void showResultButtons(String cetPercentile, String jeePercentile, String cetRank, String gender) {
        JPanel resultButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton cetPercentileBtn = createCuteButton("CET Percentile Results", new Color(135, 206, 250));
        JButton jeePercentileBtn = createCuteButton("JEE Percentile Results", new Color(255, 182, 193));
        JButton cetRankBtn = createCuteButton("CET Rank Results", new Color(240, 128, 128));
        JButton recommendBtn = createCuteButton("🏆 Top 10 Recommendations", new Color(60, 179, 113));
        JButton backBtn = createCuteButton("⬅️ Back to Dashboard", new Color(119, 136, 153));

        cetPercentileBtn.addActionListener(e -> displayResults(cetPercentile, "CET_PERCENTILE", gender));
        jeePercentileBtn.addActionListener(e -> displayResults(jeePercentile, "JEE_PERCENTILE", gender));
        cetRankBtn.addActionListener(e -> displayResults(cetRank, "CET_RANK", gender));
        recommendBtn.addActionListener(e -> displayRecommendations(cetPercentile, cetRank, jeePercentile));
        backBtn.addActionListener(e -> dashboard.showMainButtons());

        resultButtonPanel.add(cetPercentileBtn);
        resultButtonPanel.add(jeePercentileBtn);
        resultButtonPanel.add(cetRankBtn);
        resultButtonPanel.add(recommendBtn);
        resultButtonPanel.add(backBtn);

        dashboard.showPredictorResultButtons(resultButtonPanel);
    }

    private JButton createCuteButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void toggleColumn(int modelColIndex) {
        if (resultTable == null || columnChecks == null || modelColIndex < 0 || modelColIndex >= columnChecks.length || columnChecks[modelColIndex] == null) return;

        boolean visible = columnChecks[modelColIndex].isSelected();
        TableColumnModel colModel = resultTable.getColumnModel();

        if (visible) {
            TableColumn columnToAdd = null;
            for(TableColumn tc : allTableColumns){
                if(tc.getModelIndex() == modelColIndex){ columnToAdd = tc; break; }
            }
            if(columnToAdd != null){
                int visibleColsBefore = 0;
                for(int i = 0; i < modelColIndex; i++){
                    if(findViewColumnIndex(i) != -1){ visibleColsBefore++; }
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

    private boolean isColumnVisible(int modelColIndex) {
        if(resultTable == null) return false;
        TableColumnModel tcm = resultTable.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelColIndex) { return true; }
        }
        return false;
    }
    /** Finds the current view index of a column given its model index. Returns -1 if not visible. */
    private int findViewColumnIndex(int modelIndex) {
        if (resultTable == null) return -1;
        TableColumnModel tcm = resultTable.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelIndex) {
                return i;
            }
        }
        return -1;
    }


    private void updateSortOptions() {
        if (resultTable == null || sortColumn == null) return;
        sortColumn.removeAllItems();
        TableColumnModel colModel = resultTable.getColumnModel();
        TableModel model = resultTable.getModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            // Get display name (which is what user sees in checkbox/header)
            String colName = model.getColumnName(colModel.getColumn(i).getModelIndex());
            sortColumn.addItem(colName);
        }
        if (sortColumn.getItemCount() == 0) sortColumn.addItem("None");
    }

    private void applySortAction() {
        if (resultTable == null || sortColumn == null || sortColumn.getSelectedIndex() < 0 || sortColumn.getItemCount() == 0 || sorter == null) return;

        // Get the DISPLAY name selected in the dropdown
        String selectedDisplayColName = (String) sortColumn.getSelectedItem();
        int modelIndex = -1;
        TableModel currentModel = resultTable.getModel();
        // Find the model index corresponding to the DISPLAY name
        for (int i = 0; i < currentModel.getColumnCount(); i++) {
            if (currentModel.getColumnName(i).equals(selectedDisplayColName)) {
                modelIndex = i;
                break;
            }
        }

        if(modelIndex == -1) return; // Should not happen if dropdown is populated correctly

        String order = (String) sortOrder.getSelectedItem();
        if (order == null || "None".equals(order)) {
            sorter.setSortKeys(null); return;
        }

        SortOrder so = "Ascending".equals(order) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        // Apply sort based on the MODEL index
        sorter.setSortKeys(List.of(new RowSorter.SortKey(modelIndex, so)));
    }

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
                } catch (NumberFormatException e) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        }
    }
}
