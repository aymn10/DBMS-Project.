import java.sql.*;

public class DBConnection {
    static final String URL = "jdbc:mysql://localhost:3306/project";
    static final String USER = "root";
    static final String PASS = "SQL@3007";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ Database connected successfully!");
            return con;
        } catch (Exception e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
