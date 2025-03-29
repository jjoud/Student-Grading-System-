package com.mycompany.projectgrading;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CourseManagementPageForAdmin extends JPanel {
    private DefaultTableModel tableModel;

    public CourseManagementPageForAdmin() {
        // Set layout and background
        setLayout(null);
        setBackground(Color.WHITE);

        // Title Label
        JLabel labelTitle = new JLabel("Admin: Manage Courses");
        labelTitle.setFont(new Font("Arial", Font.BOLD, 18));
        labelTitle.setBounds(50, 20, 300, 30);
        add(labelTitle);

        // Courses Table
        tableModel = new DefaultTableModel(new String[]{"Course Name"}, 0);
        JTable tableCourses = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tableCourses);
        scrollPane.setBounds(50, 70, 500, 300);
        add(scrollPane);

        // Populate the table with courses
        updateCourseTable();

        // Buttons
        JButton buttonAddCourse = new JButton("Add Course");
        buttonAddCourse.setBounds(50, 400, 150, 30);
        buttonAddCourse.setFont(new Font("Arial", Font.BOLD, 14));
        buttonAddCourse.setBackground(Color.WHITE);
        buttonAddCourse.setForeground(new Color(30, 144, 255));
        add(buttonAddCourse);

        JButton buttonDeleteCourse = new JButton("Delete Course");
        buttonDeleteCourse.setBounds(220, 400, 150, 30);
        buttonDeleteCourse.setFont(new Font("Arial", Font.BOLD, 14));
        buttonDeleteCourse.setBackground(Color.WHITE);
        buttonDeleteCourse.setForeground(new Color(255, 69, 0)); // Red text
        add(buttonDeleteCourse);

        // Action Listeners
        buttonAddCourse.addActionListener(e -> addCourse());
        buttonDeleteCourse.addActionListener(e -> deleteCourse(tableCourses));
    }

    /**
     * Populates the courses table with data from the database.
     */
    private void updateCourseTable() {
        tableModel.setRowCount(0); // Clear existing rows
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT name FROM Courses");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getString("name")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching courses: " + ex.getMessage());
        }
    }

    /**
     * Adds a new course to the database.
     */
    private void addCourse() {
        JTextField courseNameField = new JTextField();

        Object[] fields = {"Course Name:", courseNameField};

        int option = JOptionPane.showConfirmDialog(this, fields, "Add Course", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = courseNameField.getText();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Course name cannot be empty.");
                return;
            }

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("INSERT INTO Courses (name) VALUES (?)")) {

                stmt.setString(1, name);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Course added successfully!");
                updateCourseTable(); // Refresh table
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding course: " + ex.getMessage());
            }
        }
    }

    /**
     * Deletes the selected course from the database.
     */
    private void deleteCourse(JTable tableCourses) {
        int selectedRow = tableCourses.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to delete.");
            return;
        }

        String courseName = (String) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the course: " + courseName + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("DELETE FROM Courses WHERE name = ?")) {

                stmt.setString(1, courseName);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Course deleted successfully!");
                updateCourseTable(); // Refresh table
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting course: " + ex.getMessage());
            }
        }
    }
    

}