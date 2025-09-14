import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

public class CollegePredictor extends JPanel {

    private JPanel formPanel;
    private JScrollPane tableScrollPane;
    private Dashboard dashboard;

    public CollegePredictor(String fname, String lname, Dashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(10, 10));

        createFormPanel(fname, lname);
        add(formPanel, BorderLayout.NORTH);

        tableScrollPane = new JScrollPane();
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private JPanel createStudentInfoPanel(String name, String gender, String cetRank, String cetPercentile, String jeeRank, String jeePercentile) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Your Submitted Data"));

        JLabel nameLabel = new JLabel("<html><b>Name:</b> " + (name.isEmpty() ? "N/A" : name) + "</html>");
        JLabel genderLabel = new JLabel("<html><b>Gender:</b> " + (gender.isEmpty() ? "N/A" : gender) + "</html>");
        JLabel cetRankLabel = new JLabel("<html><b>CET Rank:</b> " + (cetRank.isEmpty() ? "N/A" : cetRank) + "</html>");
        JLabel cetPercentileLabel = new JLabel("<html><b>CET Percentile:</b> " + (cetPercentile.isEmpty() ? "N/A" : cetPercentile) + "</html>");
        JLabel jeeRankLabel = new JLabel("<html><b>JEE Rank:</b> " + (jeeRank.isEmpty() ? "N/A" : jeeRank) + "</html>");
        JLabel jeePercentileLabel = new JLabel("<html><b>JEE Percentile:</b> " + (jeePercentile.isEmpty() ? "N/A" : jeePercentile) + "</html>");

        panel.add(nameLabel);
        panel.add(genderLabel);
        panel.add(cetRankLabel);
        panel.add(cetPercentileLabel);
        panel.add(jeeRankLabel);
        panel.add(jeePercentileLabel);
        return panel;
    }

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
            this.add(tableScrollPane, BorderLayout.CENTER);
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

    private void displayResults(String score, String type, String gender) {
        if (score == null || score.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not enter a score for this category.", "Missing Score", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "";
        boolean isFemale = "Female".equalsIgnoreCase(gender);

        switch (type) {
            case "CET_PERCENTILE":
                if (isFemale) {
                    sql = "SELECT ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, pc.CAP1_Percentile, pc.CAP2_Percentile, pc.CAP3_Percentile, pc.CAP_LOPEN1_Percentile, pc.CAP_LOPEN2_Percentile, pc.CAP_LOPEN3_Percentile " +
                            "FROM PercentileCutoff pc " +
                            "JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode " +
                            "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                            "WHERE LEAST(IFNULL(pc.CAP1_Percentile, 101), IFNULL(pc.CAP2_Percentile, 101), IFNULL(pc.CAP3_Percentile, 101), IFNULL(pc.CAP_LOPEN1_Percentile, 101), IFNULL(pc.CAP_LOPEN2_Percentile, 101), IFNULL(pc.CAP_LOPEN3_Percentile, 101)) <= ? " +
                            "ORDER BY ci.InstituteName, bf.CourseName";
                } else {
                    sql = "SELECT ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, pc.CAP1_Percentile, pc.CAP2_Percentile, pc.CAP3_Percentile " +
                            "FROM PercentileCutoff pc " +
                            "JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode " +
                            "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                            "WHERE LEAST(IFNULL(pc.CAP1_Percentile, 101), IFNULL(pc.CAP2_Percentile, 101), IFNULL(pc.CAP3_Percentile, 101)) <= ? " +
                            "ORDER BY ci.InstituteName, bf.CourseName";
                }
                break;

            case "JEE_PERCENTILE":
                // ✅ CORRECTED: This query now selects JEE_Rank as requested
                sql = "SELECT ci.InstituteName, bf.CourseName, ci.Location, bf.Fees, jc.JEE_Percentile, jc.JEE_Rank " +
                        "FROM jeeCutoff jc " +
                        "JOIN BranchFees bf ON jc.ChoiceCode = bf.ChoiceCode " +
                        "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                        "WHERE jc.JEE_Percentile <= ? ORDER BY jc.JEE_Percentile DESC";
                break;

            case "CET_RANK":
                if (isFemale) {
                    sql = "SELECT ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, c.CAP1_Rank, c.CAP2_Rank, c.CAP3_Rank, c.CAP_LOPEN1_Rank, c.CAP_LOPEN2_Rank, c.CAP_LOPEN3_Rank " +
                            "FROM cet_rankcutoff c " +
                            "JOIN BranchFees bf ON c.Choice_Code = bf.ChoiceCode " +
                            "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                            "WHERE GREATEST(IFNULL(c.CAP1_Rank, 0), IFNULL(c.CAP2_Rank, 0), IFNULL(c.CAP3_Rank, 0), IFNULL(c.CAP_LOPEN1_Rank, 0), IFNULL(c.CAP_LOPEN2_Rank, 0), IFNULL(c.CAP_LOPEN3_Rank, 0)) >= ? " +
                            "ORDER BY ci.InstituteName, bf.CourseName";
                } else {
                    sql = "SELECT ci.InstituteName, bf.CourseName, bf.Fees, ci.Location, c.CAP1_Rank, c.CAP2_Rank, c.CAP3_Rank " +
                            "FROM cet_rankcutoff c " +
                            "JOIN BranchFees bf ON c.Choice_Code = bf.ChoiceCode " +
                            "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                            "WHERE GREATEST(IFNULL(c.CAP1_Rank, 0), IFNULL(c.CAP2_Rank, 0), IFNULL(c.CAP3_Rank, 0)) >= ? " +
                            "ORDER BY ci.InstituteName, bf.CourseName";
                }
                break;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDouble(1, Double.parseDouble(score));
            ResultSet rs = pst.executeQuery();
            DefaultTableModel tableModel = buildTableModel(rs);

            if(tableModel.getRowCount() == 0){
                JOptionPane.showMessageDialog(this, "No colleges found matching your score.", "No Results", JOptionPane.INFORMATION_MESSAGE);
            }

            JTable resultTable = new JTable(tableModel);
            resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            resizeColumnWidths(resultTable);
            resultTable.setFillsViewportHeight(true);

            tableScrollPane.setViewportView(resultTable);
            tableScrollPane.revalidate();
            tableScrollPane.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching prediction: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void resizeColumnWidths(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 150;

            TableColumn tableColumn = columnModel.getColumn(column);
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = headerRenderer.getTableCellRendererComponent(table, tableColumn.getHeaderValue(), false, false, 0, column);
            width = Math.max(width, headerComp.getPreferredSize().width + 10);

            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column).replace("_", " "));
        }
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }
        return new DefaultTableModel(data, columnNames);
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

        gbc.gridy = 1;
        gbc.gridx = 0;
        formPanel.add(new JLabel("CET Rank:"), gbc);
        gbc.gridx = 1;
        JTextField cetRankField = new JTextField(15);
        formPanel.add(cetRankField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("CET Percentile:"), gbc);
        gbc.gridx = 3;
        JTextField cetPercentileField = new JTextField(15);
        formPanel.add(cetPercentileField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
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
        JButton backBtn = createCuteButton("⬅️ Back to Dashboard", new Color(119, 136, 153));

        cetPercentileBtn.addActionListener(e -> displayResults(cetPercentile, "CET_PERCENTILE", gender));
        jeePercentileBtn.addActionListener(e -> displayResults(jeePercentile, "JEE_PERCENTILE", gender));
        cetRankBtn.addActionListener(e -> displayResults(cetRank, "CET_RANK", gender));
        backBtn.addActionListener(e -> dashboard.showMainButtons());

        resultButtonPanel.add(cetPercentileBtn);
        resultButtonPanel.add(jeePercentileBtn);
        resultButtonPanel.add(cetRankBtn);
        resultButtonPanel.add(backBtn);

        dashboard.showPredictorResultButtons(resultButtonPanel);
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
}