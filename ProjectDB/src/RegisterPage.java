import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterPage extends JFrame {
    JTextField fnameField, lnameField, emailField, mobileField;
    JPasswordField pinField;
    JComboBox<String> genderBox;
    JButton registerBtn, loginBtn;

    public RegisterPage() {
        setTitle(" Register Account ");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel(" Create Your Account ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        fnameField = new JTextField();
        fnameField.setPreferredSize(new Dimension(150, 30));
        add(fnameField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lnameField = new JTextField();
        lnameField.setPreferredSize(new Dimension(150, 30));
        add(lnameField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("Gender:"), gbc);
        gbc.gridx = 1;
        genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        genderBox.setPreferredSize(new Dimension(150, 30));
        add(genderBox, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(150, 30));
        add(emailField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("Mobile:"), gbc);
        gbc.gridx = 1;
        mobileField = new JTextField();
        mobileField.setPreferredSize(new Dimension(150, 30));
        add(mobileField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        add(new JLabel("PIN:"), gbc);
        gbc.gridx = 1;
        pinField = new JPasswordField();
        pinField.setPreferredSize(new Dimension(150, 30));
        add(pinField, gbc);

        // Buttons
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        registerBtn = createCuteButton("Register", new Color(135, 206, 250));
        loginBtn = createCuteButton("Go to Login", new Color(255, 182, 193));

        btnPanel.add(registerBtn);
        btnPanel.add(loginBtn);
        add(btnPanel, gbc);

        // Actions
        registerBtn.addActionListener(e -> registerUser());
        loginBtn.addActionListener(e -> {
            dispose();
            new LoginPage().setVisible(true);
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

    private void registerUser() {
        String fname = fnameField.getText();
        String lname = lnameField.getText();
        String gender = (String) genderBox.getSelectedItem();
        String email = emailField.getText();
        String mobile = mobileField.getText();
        String pin = new String(pinField.getPassword());

        Connection con = DBConnection.getConnection();
        if (con == null) {
            JOptionPane.showMessageDialog(this,
                    "❌ Database connection failed.\nPlease check MySQL server, credentials, or JDBC driver!");
            return;
        }

        try {
            String sql = "INSERT INTO users (firstname, lastname, gender, email, mobile, pin) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, fname);
            pst.setString(2, lname);
            pst.setString(3, gender);
            pst.setString(4, email);
            pst.setString(5, mobile);
            pst.setString(6, pin);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Registration Successful!");
            con.close();
            dispose();
            new LoginPage().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterPage().setVisible(true));
    }
}
