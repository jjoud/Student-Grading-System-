package com.mycompany.projectgrading;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;

class RegistrationPage extends JFrame {
    private JLabel labelWelcome, labelName, labelEmail, labelPassword, labelConfirmPassword, labelRole;
    private JTextField textName, textEmail;
    private JPasswordField textPassword, textConfirmPassword;
    private JComboBox<String> comboBoxRole;
    private JButton buttonRegister, buttonBack;

    public RegistrationPage() {
        setTitle("Registration Page");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(null);
        leftPanel.setPreferredSize(new Dimension(600, 600));

        labelWelcome = new JLabel("Create Account");
        labelWelcome.setFont(new Font("Arial", Font.BOLD, 24));
        labelWelcome.setBounds(150, 20, 200, 30);
        leftPanel.add(labelWelcome);

        labelName = new JLabel("Name:");
        labelName.setBounds(50, 80, 300, 20);
        textName = new JTextField();
        textName.setBounds(50, 110, 400, 30);
        leftPanel.add(labelName);
        leftPanel.add(textName);

        labelEmail = new JLabel("Email:");
        labelEmail.setBounds(50, 160, 300, 20);
        textEmail = new JTextField();
        textEmail.setBounds(50, 190, 400, 30);
        leftPanel.add(labelEmail);
        leftPanel.add(textEmail);

        labelPassword = new JLabel("Password:");
        labelPassword.setBounds(50, 240, 300, 20);
        textPassword = new JPasswordField();
        textPassword.setBounds(50, 270, 400, 30);
        leftPanel.add(labelPassword);
        leftPanel.add(textPassword);

        labelConfirmPassword = new JLabel("Confirm Password:");
        labelConfirmPassword.setBounds(50, 320, 300, 20);
        textConfirmPassword = new JPasswordField();
        textConfirmPassword.setBounds(50, 350, 400, 30);
        leftPanel.add(labelConfirmPassword);
        leftPanel.add(textConfirmPassword);

        labelRole = new JLabel("Select Role:");
        labelRole.setBounds(50, 400, 300, 20);
        comboBoxRole = new JComboBox<>(new String[]{"Student", "Teacher"});
        comboBoxRole.setBounds(50, 430, 400, 30);
        leftPanel.add(labelRole);
        leftPanel.add(comboBoxRole);

        buttonRegister = new JButton("Register");
        buttonRegister.setBounds(50, 490, 180, 40);
        buttonRegister.addActionListener(e -> registerAction());
        leftPanel.add(buttonRegister);

        buttonBack = new JButton("Back");
        buttonBack.setBounds(270, 490, 180, 40);
        buttonBack.addActionListener(e -> backAction());
        leftPanel.add(buttonBack);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(173, 216, 230));
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, 600));

        JLabel labelSystemTitle = new JLabel("Student Grading System");
        labelSystemTitle.setFont(new Font("Arial", Font.BOLD, 18));
        rightPanel.add(labelSystemTitle);

        return rightPanel;
    }

    private void registerAction() {
        String name = textName.getText().trim();
        String email = textEmail.getText().trim();
        String password = new String(textPassword.getPassword()).trim();
        String confirmPassword = new String(textConfirmPassword.getPassword()).trim();
        String role = (String) comboBoxRole.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Invalid email format.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        if (!isValidPassword(password)) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters long, include an uppercase letter, and a number.");
            return;
        }

        try {
            registerUser(name, email, password, role);
            JOptionPane.showMessageDialog(this, role + " registered successfully!");
            new LoginPage().setVisible(true);
            dispose();
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "Email is already registered.");
            } else {
                JOptionPane.showMessageDialog(this, "Error registering " + role + ": " + ex.getMessage());
            }
        }
    }

    private void backAction() {
        new LoginPage().setVisible(true);
        dispose();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*\\d.*");
    }

    private void registerUser(String name, String email, String password, String role) throws SQLException {
        String query = "INSERT INTO Users (name, email, password, role) VALUES (?, ?, ?, ?)";
        String studentQuery = "INSERT INTO Students (student_id, name, email) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next() && "Student".equalsIgnoreCase(role)) {
                int userId = rs.getInt(1);
                try (PreparedStatement studentStmt = connection.prepareStatement(studentQuery)) {
                    studentStmt.setInt(1, userId);
                    studentStmt.setString(2, name);
                    studentStmt.setString(3, email);
                    studentStmt.executeUpdate();
                }
            }
        }
    }
}
