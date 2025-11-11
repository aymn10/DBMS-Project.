import javax.swing.*;
import java.awt.*;
import java.sql.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class AnalyticsPage extends JFrame {

    public AnalyticsPage() {
        setTitle(" 📊 Project Analytics ");
        setSize(1200, 700); // Made window wider
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Chart 1: Colleges per University (Vertical Bar Chart) ---
        JPanel barChartPanel1 = createUniversityBarChart();
        tabbedPane.addTab("Colleges by University", barChartPanel1);

        // --- Chart 2: Average Fees by Course (Horizontal Bar Chart) ---
        JPanel barChartPanel2 = createFeeBarChart();
        tabbedPane.addTab("Avg. Fees by Course", barChartPanel2);

        // --- Chart 3: Cutoff Analysis (Horizontal Bar Chart) ---
        JPanel barChartPanel3 = createCutoffBarChart();
        tabbedPane.addTab("Cutoff Analysis", barChartPanel3);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Chart 1: Shows Top Universities by College Count
     */
    private JPanel createUniversityBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT University, COUNT(*) AS CollegeCount " +
                "FROM CollegeInfo " +
                "GROUP BY University " +
                "ORDER BY CollegeCount DESC " +
                "LIMIT 10";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String university = rs.getString("University");
                int count = rs.getInt("CollegeCount");
                dataset.addValue(count, "Colleges", university);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error (Chart 1): " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 10 Universities by College Count",
                "University",
                "Number of Colleges",
                dataset,
                PlotOrientation.VERTICAL, // Vertical for this one is fine
                false, true, false);

        // --- Custom Colors ---
        BarRenderer renderer = (BarRenderer) barChart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, new Color(70, 130, 180)); // Nice blue color

        return new ChartPanel(barChart);
    }

    /**
     * Chart 2: Shows Average Fees by Course (FIXED: Now a Horizontal Bar Chart)
     */
    private JPanel createFeeBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT CourseName, AVG(Fees) AS AvgFee " +
                "FROM BranchFees " +
                "GROUP BY CourseName " +
                "HAVING AvgFee > 50000 " + // Filter for more relevant data
                "ORDER BY AvgFee DESC";     // Order by fee

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String course = rs.getString("CourseName");
                double avgFee = rs.getDouble("AvgFee");
                // Add data (value, rowKey, columnKey)
                dataset.addValue(avgFee, "Average Fee", course);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error (Chart 2): " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Average Fees by Course (Fees > 50k)",
                "Course Name",  // X-axis (now on the side)
                "Average Fee (INR)", // Y-axis (now at the bottom)
                dataset,
                PlotOrientation.HORIZONTAL, // <-- THE FIX: Changed to Horizontal
                false, true, false);

        // --- Custom Colors ---
        BarRenderer renderer = (BarRenderer) barChart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, new Color(60, 179, 113)); // Nice green color

        return new ChartPanel(barChart);
    }

    /**
     * Chart 3: NEW Chart for Cutoff Analysis
     * Shows Top 15 CET Percentile Cutoffs for Computer Engineering
     */
    private JPanel createCutoffBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // This is a 3-table join, a great OLAP example
        String sql = "SELECT ci.InstituteName, pc.CAP1_Percentile " +
                "FROM PercentileCutoff pc " +
                "JOIN BranchFees bf ON pc.ChoiceCode = bf.ChoiceCode " +
                "JOIN CollegeInfo ci ON bf.InstituteCode = ci.InstituteCode " +
                "WHERE bf.CourseName = 'Computer Engineering' AND pc.CAP1_Percentile IS NOT NULL " +
                "ORDER BY pc.CAP1_Percentile DESC " +
                "LIMIT 15"; // Get Top 15

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String college = rs.getString("InstituteName");
                double percentile = rs.getDouble("CAP1_Percentile");
                dataset.addValue(percentile, "CET Percentile", college);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error (Chart 3): " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 15 CET Cutoffs for 'Computer Engineering'",
                "College",
                "CAP-1 Percentile",
                dataset,
                PlotOrientation.HORIZONTAL, // Horizontal is best for long college names
                false, true, false);

        // --- Custom Colors ---
        BarRenderer renderer = (BarRenderer) barChart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, new Color(218, 112, 214)); // Nice orchid color

        return new ChartPanel(barChart);
    }

    public static void main(String[] args) {
        // For testing this page directly
        SwingUtilities.invokeLater(() -> new AnalyticsPage().setVisible(true));
    }
}