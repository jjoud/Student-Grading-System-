package com.mycompany.projectgrading;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JLabel labelWelcome, labelEmail, labelPassword;
    private JTextField textEmail;
    private JPasswordField textPassword;
    private JButton buttonLogin, buttonRegister;

    public LoginPage() {
        // Set up the JFrame
        setTitle("Login Page");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout()); // Use BorderLayout for two sections

        // Left Panel (White Background)
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(null); // Absolute layout for custom positioning
        leftPanel.setPreferredSize(new Dimension(600, 600)); // Set width

        // Welcome label
        labelWelcome = new JLabel("Welcome Back!");
        labelWelcome.setFont(new Font("Arial", Font.BOLD, 24));
        labelWelcome.setForeground(Color.BLACK);
        labelWelcome.setBounds(150, 20, 200, 30);
        leftPanel.add(labelWelcome);

        // Email label and text field
        labelEmail = new JLabel("Email:");
        labelEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        labelEmail.setForeground(Color.BLACK);
        labelEmail.setBounds(50, 100, 300, 20);
        leftPanel.add(labelEmail);

        textEmail = new JTextField();
        textEmail.setBounds(50, 130, 400, 30);
        textEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        leftPanel.add(textEmail);

        // Password label and text field
        labelPassword = new JLabel("Password:");
        labelPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        labelPassword.setForeground(Color.BLACK);
        labelPassword.setBounds(50, 190, 300, 20);
        leftPanel.add(labelPassword);

        textPassword = new JPasswordField();
        textPassword.setBounds(50, 220, 400, 30);
        textPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        leftPanel.add(textPassword);

        // Buttons
        buttonLogin = new JButton("Login");
        buttonLogin.setBounds(50, 280, 180, 40);
        buttonLogin.setFont(new Font("Arial", Font.BOLD, 14));
        buttonLogin.setBackground(new Color(255, 165, 0)); // Orange
        buttonLogin.setForeground(Color.BLACK);
        leftPanel.add(buttonLogin);

        buttonRegister = new JButton("Register");
        buttonRegister.setBounds(270, 280, 180, 40);
        buttonRegister.setFont(new Font("Arial", Font.BOLD, 14));
        buttonRegister.setBackground(Color.WHITE);
        buttonRegister.setForeground(new Color(30, 144, 255)); // Blue text
        leftPanel.add(buttonRegister);

        // Right Panel (Light Blue Background)
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(173, 216, 230)); // Light blue
        rightPanel.setLayout(new GridBagLayout()); // Center-align content
        rightPanel.setPreferredSize(new Dimension(300, 600));

        JLabel labelSystemTitle = new JLabel("Student Grading System");
        labelSystemTitle.setFont(new Font("Arial", Font.BOLD, 18));
        labelSystemTitle.setForeground(Color.BLACK);
        rightPanel.add(labelSystemTitle);

        // Add panels to the frame
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        // Action Listeners
        buttonLogin.addActionListener(e -> {
            String email = textEmail.getText().trim();
            String password = new String(textPassword.getPassword()).trim();

            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Email and Password are required.");
                return;
            }

            try {
                System.out.println("Attempting login with email: " + email);
                
                // Validate user credentials and get role
                User user = authenticateUser(email, password);
                if (user != null) {
                    System.out.println("User authenticated: " + user.getName() + ", Role: " + user.getRole());
                    
                    // Open corresponding dashboard based on role
                    switch (user.getRole()) {
                        case "Student":
                            new StudentDashboard(user).setVisible(true);
                            break;
                        case "Teacher":
                            new TeacherDashboard(user).setVisible(true);
                            break;
                        case "Admin":
                            new AdminDashboard(user).setVisible(true);
                            break;
                        default:
                            JOptionPane.showMessageDialog(this, "Invalid role assigned to user.");
                            return;
                    }
                    dispose(); // Close login page
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials","Error", JOptionPane.ERROR_MESSAGE );
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        buttonRegister.addActionListener(e -> {
            new RegistrationPage().setVisible(true);
            dispose(); // Close login page
        });
    }

    public static void main(String[] args) {
        new LoginPage().setVisible(true);
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("Connected to the database!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User authenticateUser(String email, String password) throws SQLException {
        String query = "SELECT user_id, name, email, role FROM Users WHERE email = ? AND password = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role")
                );
            }
        }
        return null; // No matching user
    }
}
