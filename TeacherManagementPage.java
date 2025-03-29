package com.mycompany.projectgrading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class TeacherManagementPage {
    private User currentUser;
    private DefaultTableModel tableModel;
    private JPanel mainPanel;

    public TeacherManagementPage(User user) {
        this.currentUser = user;

        // Main Panel Setup
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Title Label
        JLabel titleLabel = new JLabel("Manage Teachers", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Teachers Table
        tableModel = new DefaultTableModel(new String[]{"Teacher Name", "Email", "Assigned Courses"}, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton assignButton = createButton("Assign Courses", e -> assignCoursesToTeacher());
        JButton editButton = createButton("Edit Teacher", e -> editTeacher(table));
        JButton deleteButton = createButton("Delete Teacher", e -> deleteTeacher(table));

        buttonPanel.add(assignButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load teachers into the table
        loadTeacherData();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    private void loadTeacherData() {
        tableModel.setRowCount(0);
        String query = "SELECT t.name, t.email, " +
                       "(SELECT GROUP_CONCAT(c.name SEPARATOR ', ') FROM Assignments a " +
                       "JOIN Courses c ON a.course_id = c.course_id WHERE a.teacher_id = t.user_id) AS courses " +
                       "FROM Users t WHERE t.role = 'Teacher'";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("courses") != null ? rs.getString("courses") : "Not Assigned"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading teachers: " + ex.getMessage());
        }
    }

    private void assignCoursesToTeacher() {
    try {
        ArrayList<String[]> teachers = fetchAllTeachers();
        ArrayList<String[]> courses = fetchAllCourses();

        if (teachers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No teachers available.");
            return;
        }

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No courses available.");
            return;
        }

        JComboBox<String> teacherComboBox = new JComboBox<>(teachers.stream().map(t -> t[1]).toArray(String[]::new));
        JList<String> courseList = new JList<>(courses.stream().map(c -> c[1]).toArray(String[]::new));
        courseList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Select Teacher:"), BorderLayout.NORTH);
        panel.add(teacherComboBox, BorderLayout.CENTER);
        panel.add(new JScrollPane(courseList), BorderLayout.SOUTH);

        if (JOptionPane.showConfirmDialog(null, panel, "Assign Courses to Teacher", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int teacherId = Integer.parseInt(teachers.get(teacherComboBox.getSelectedIndex())[0]);
            boolean anyAssigned = false; // Track if any course was successfully assigned

            for (int index : courseList.getSelectedIndices()) {
                int courseId = Integer.parseInt(courses.get(index)[0]);
                if (assignTeacherToCourse(teacherId, courseId)) {
                    anyAssigned = true; // Mark as successfully assigned
                }
            }

            if (anyAssigned) {
                JOptionPane.showMessageDialog(null, "Courses assigned to teacher successfully.");
            }

            loadTeacherData(); // Refresh the table
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error assigning courses: " + ex.getMessage());
    }
}

    
    private boolean isCourseAlreadyAssigned(int courseId) throws SQLException {
    String query = "SELECT COUNT(*) FROM Assignments WHERE course_id = ?";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setInt(1, courseId);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0; // Returns true if the course is already assigned
            }
        }
    }
    return false; // Returns false if the course is not assigned
}

                  
    private boolean assignTeacherToCourse(int teacherId, int courseId) throws SQLException {
    String checkQuery = "SELECT COUNT(*) FROM Assignments WHERE course_id = ?";
    String insertQuery = "INSERT INTO Assignments (teacher_id, course_id) VALUES (?, ?)";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {

        // Check if the course is already assigned to a teacher
        checkStmt.setInt(1, courseId);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null, "This course is already assigned to a teacher.");
                return false; // Return false to indicate the assignment was not successful
            }
        }

        // Assign the course to the teacher
        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setInt(1, teacherId);
            insertStmt.setInt(2, courseId);
            insertStmt.executeUpdate();
            return true; // Return true to indicate the assignment was successful
        }
    }
}





    private ArrayList<String[]> fetchAllTeachers() throws SQLException {
        ArrayList<String[]> teachers = new ArrayList<>();
        String query = "SELECT user_id, name FROM Users WHERE role = 'Teacher'";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teachers.add(new String[]{String.valueOf(rs.getInt("user_id")), rs.getString("name")});
            }
        }

        return teachers;
    }

    private ArrayList<String[]> fetchAllCourses() throws SQLException {
        ArrayList<String[]> courses = new ArrayList<>();
        String query = "SELECT course_id, name FROM Courses";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                courses.add(new String[]{String.valueOf(rs.getInt("course_id")), rs.getString("name")});
            }
        }

        return courses;
    }

    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(30, 144, 255));
        button.addActionListener(listener);
        return button;
    }
    private boolean emailExists(String email) throws SQLException {
    String query = "SELECT COUNT(*) FROM Users WHERE email = ? AND role = 'Teacher'";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setString(1, email);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0; // Returns true if the email is already in the database
            }
        }
    }
    return false; // Returns false if the email is not found
}

    private void editTeacher(JTable table) {
    int selectedRow = table.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Please select a teacher to edit.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    String currentName = (String) tableModel.getValueAt(selectedRow, 0);
    String currentEmail = (String) tableModel.getValueAt(selectedRow, 1);

    JTextField nameField = new JTextField(currentName);
    JTextField emailField = new JTextField(currentEmail);

    Object[] fields = {
        "Name:", nameField,
        "Email:", emailField
    };

    if (JOptionPane.showConfirmDialog(null, fields, "Edit Teacher", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        String newName = nameField.getText().trim();
        String newEmail = emailField.getText().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Name and email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Check if the new email already exists for another teacher
            if (!newEmail.equals(currentEmail) && emailExists(newEmail)) {
                JOptionPane.showMessageDialog(null, "The email \"" + newEmail + "\" is already in use.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update the teacher's details in the database
            String query = "UPDATE Users SET name = ?, email = ? WHERE email = ?";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, newName);
                stmt.setString(2, newEmail);
                stmt.setString(3, currentEmail);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Teacher updated successfully.");
                loadTeacherData();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error updating teacher: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}


    private void deleteTeacher(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a teacher to delete.");
            return;
        }

        String email = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this teacher?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM Users WHERE email = ?";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, email);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Teacher deleted successfully.");
                loadTeacherData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error deleting teacher: " + ex.getMessage());
            }
        }
    }

    

    

    
}