import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginPage extends JFrame {
    JTextField mobileField;
    JPasswordField pinField;
    JButton loginBtn, registerBtn;

    public LoginPage() {
        setTitle("Login Page ");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel(" Welcome Back ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        add(new JLabel("📱 Mobile:"), gbc);
        gbc.gridx = 1;
        mobileField = new JTextField();
        mobileField.setPreferredSize(new Dimension(150, 30));
        add(mobileField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("🔑 PIN:"), gbc);
        gbc.gridx = 1;
        pinField = new JPasswordField();
        pinField.setPreferredSize(new Dimension(150, 30));
        add(pinField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        loginBtn = createCuteButton("Login", new Color(135, 206, 250));
        registerBtn = createCuteButton("Register", new Color(255, 182, 193));

        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);
        add(btnPanel, gbc);

        loginBtn.addActionListener(e -> loginUser());
        registerBtn.addActionListener(e -> {
            dispose();
            new RegisterPage().setVisible(true);
        });
    }

    private JButton createCuteButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loginUser() {
        String mobile = mobileField.getText();
        String pin = new String(pinField.getPassword());

        Connection con = DBConnection.getConnection();
        if (con == null) {
            JOptionPane.showMessageDialog(this,
                    "❌ Database connection failed.\nPlease check MySQL server, credentials, or JDBC driver!");
            return;
        }

        try {
            String sql = "SELECT * FROM users WHERE mobile=? AND pin=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, mobile);
            pst.setString(2, pin);  

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "✅ Login Successful!");
                dispose();
                new Dashboard(rs.getString("firstname"), rs.getString("lastname")).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Invalid Mobile or PIN");
            }
            con.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}


