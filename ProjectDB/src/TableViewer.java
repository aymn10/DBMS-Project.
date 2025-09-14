import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public class TableViewer extends JFrame {
    private JTable table;
    private JComboBox<String> filterColumn, sortColumn, sortOrder;
    private JTextField filterText;
    private TableRowSorter<TableModel> sorter;

    public TableViewer(String tableName) {
        setTitle("Viewing: " + tableName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Fetch data
        DefaultTableModel model = fetchTableData(tableName);

        table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);   // we will size columns ourselves
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Filter + Sort Panel (top)
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

        topPanel.add(new JLabel("Search:"));
        topPanel.add(filterText);
        topPanel.add(applyFilter);
        topPanel.add(reset);
        topPanel.add(new JLabel("Sort:"));
        topPanel.add(sortColumn);
        topPanel.add(sortOrder);
        topPanel.add(applySort);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(
                table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ), BorderLayout.CENTER);

        // Actions
        applyFilter.addActionListener(e -> applyFilterAction());
        reset.addActionListener(e -> {
            filterText.setText("");
            sorter.setRowFilter(null);
            sortOrder.setSelectedIndex(0); // None
            sorter.setSortKeys(null);
        });
        applySort.addActionListener(e -> applySortAction());

        // Fit columns once table is realized
        SwingUtilities.invokeLater(() -> resizeColumnsToFitContent(table));
    }

    private DefaultTableModel fetchTableData(String tableName) {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {

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

            return new DefaultTableModel(data, columnNames);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading table: " + e.getMessage());
            e.printStackTrace();
            return new DefaultTableModel(); // empty model on error
        }
    }

    private void applyFilterAction() {
        String text = filterText.getText().trim();
        int colIndex = Math.max(0, filterColumn.getSelectedIndex());

        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        // Use plain-text search (escape regex special characters)
        String pattern = "(?i)" + Pattern.quote(text);
        sorter.setRowFilter(RowFilter.regexFilter(pattern, colIndex));
    }

    private void applySortAction() {
        int colIndex = Math.max(0, sortColumn.getSelectedIndex());
        String order = (String) sortOrder.getSelectedItem();
        if (order == null || "None".equals(order)) {
            sorter.setSortKeys(null);
            return;
        }

        // Numeric-first comparator; fallback to case-insensitive string compare
        sorter.setComparator(colIndex, (o1, o2) -> {
            String s1 = o1 == null ? "" : o1.toString();
            String s2 = o2 == null ? "" : o2.toString();
            try {
                long n1 = Long.parseLong(s1.replaceAll("[^0-9-]", ""));
                long n2 = Long.parseLong(s2.replaceAll("[^0-9-]", ""));
                return Long.compare(n1, n2);
            } catch (NumberFormatException ex) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        SortOrder so = "Ascending".equals(order) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(colIndex, so)));
    }

    /** Auto-resize columns to fit header + widest cell, while still allowing manual resize. */
    private void resizeColumnsToFitContent(JTable tbl) {
        TableColumnModel colModel = tbl.getColumnModel();
        JTableHeader header = tbl.getTableHeader();

        for (int col = 0; col < tbl.getColumnCount(); col++) {
            TableColumn column = colModel.getColumn(col);
            int maxWidth = 100; // minimal width

            // Header width
            TableCellRenderer hdrRenderer = column.getHeaderRenderer();
            if (hdrRenderer == null) hdrRenderer = header.getDefaultRenderer();
            Component hdrComp = hdrRenderer.getTableCellRendererComponent(tbl, column.getHeaderValue(), false, false, 0, col);
            maxWidth = Math.max(maxWidth, hdrComp.getPreferredSize().width + 18);

            // Cell widths
            for (int row = 0; row < tbl.getRowCount(); row++) {
                TableCellRenderer cellRenderer = tbl.getCellRenderer(row, col);
                Component c = tbl.prepareRenderer(cellRenderer, row, col);
                maxWidth = Math.max(maxWidth, c.getPreferredSize().width + 18);
            }

            column.setPreferredWidth(maxWidth);
        }
    }
}
