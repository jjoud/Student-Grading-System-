package com.mycompany.projectgrading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class StudentManagementPage {
    private User currentUser;
    private DefaultTableModel tableModel;
    private JPanel mainPanel;

    public StudentManagementPage(User user) {
        this.currentUser = user;

        // Main Panel Setup
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Title Label
        JLabel titleLabel = new JLabel("Manage Students", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Students Table
        tableModel = new DefaultTableModel(new String[]{"Student Name", "Email", "Enrolled Courses"}, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(createButton("Enroll Courses", e -> enrollStudentToCourses()));
        buttonPanel.add(createButton("Edit Student", e -> editStudent(table)));
        buttonPanel.add(createButton("Remove Enrollment", e -> removeEnrollment(table)));
        buttonPanel.add(createButton("Generate Report", e -> generateStudentReports()));

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load students into the table
        loadStudentData();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    private void loadStudentData() {
        tableModel.setRowCount(0);
        String query = "SELECT s.name, s.email, " +
                "(SELECT GROUP_CONCAT(c.name SEPARATOR ', ') " +
                " FROM Enrollments e JOIN Courses c ON e.course_id = c.course_id WHERE e.student_id = s.student_id) AS courses " +
                "FROM Students s";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("courses") != null ? rs.getString("courses") : "Not Enrolled"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading students: " + ex.getMessage());
        }
    }

    private void generateStudentReports() {
        try {
            ArrayList<String[]> students = fetchAllStudents();

            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No students available.");
                return;
            }

            JList<String> studentList = new JList<>(students.stream().map(s -> s[1]).toArray(String[]::new));
            studentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("Select Student(s):"), BorderLayout.NORTH);
            panel.add(new JScrollPane(studentList), BorderLayout.CENTER);

            if (JOptionPane.showConfirmDialog(null, panel, "Generate Reports", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                for (int index : studentList.getSelectedIndices()) {
                    int studentId = Integer.parseInt(students.get(index)[0]);
                    generateStudentReport(studentId);
                }

                JOptionPane.showMessageDialog(null, "Reports generated successfully.");
            }
        } catch (SQLException | IOException ex) {
            JOptionPane.showMessageDialog(null, "Error generating reports: " + ex.getMessage());
        }
    }

    private void generateStudentReport(int studentId) throws SQLException, IOException {
    String query = "SELECT s.name AS student_name, s.email AS email, " +
            "c.name AS course_name, c.credit_hours, " +
            "(SELECT u.name FROM Users u JOIN Assignments a ON u.user_id = a.teacher_id WHERE a.course_id = c.course_id LIMIT 1) AS instructor_name, " +
            "g.assignment_score, g.quiz_score, g.exam_score, " +
            "ROUND((g.assignment_score * c.assignment_weight / 100 + g.quiz_score * c.quiz_weight / 100 + g.exam_score * c.exam_weight / 100), 2) AS final_grade " +
            "FROM Students s " +
            "JOIN Enrollments e ON s.student_id = e.student_id " +
            "JOIN Courses c ON e.course_id = c.course_id " +
            "LEFT JOIN Grades g ON g.course_id = c.course_id AND g.student_id = s.student_id " +
            "WHERE s.student_id = ?";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setInt(1, studentId);
        ResultSet rs = stmt.executeQuery();

        StringBuilder reportContent = new StringBuilder();
        reportContent.append("------------------------------------------------------\n")
                .append("                 STUDENT TRANSCRIPT\n")
                .append("------------------------------------------------------\n")
                .append("Student ID: ").append(studentId).append("\n\n")
                .append("| Course Name   | Credit Hours | Instructor   | Assignment | Quiz | Exam | Final Grade |\n")
                .append("------------------------------------------------------\n");

        int totalCreditHours = 0;
        double totalQualityPoints = 0;
        int totalCourses = 0;

        while (rs.next()) {
            String courseName = rs.getString("course_name");
            int creditHours = rs.getInt("credit_hours");
            String instructorName = rs.getString("instructor_name") != null ? rs.getString("instructor_name") : "Unassigned";
            int assignmentScore = rs.getInt("assignment_score");
            int quizScore = rs.getInt("quiz_score");
            int examScore = rs.getInt("exam_score");
            double finalGrade = rs.getDouble("final_grade");

            // Compute letter grade
            String letterGrade = getLetterGrade(finalGrade);

            // Compute quality points
            double gradePoint = getGradePoint(finalGrade); // Convert percentage to grade point
            double qualityPoints = gradePoint * creditHours;

            totalCreditHours += creditHours;
            totalQualityPoints += qualityPoints;
            totalCourses++;

            reportContent.append(String.format("| %-13s | %-12d | %-12s | %-10d | %-4d | %-4d | %-10s |\n",
                    courseName, creditHours, instructorName, assignmentScore, quizScore, examScore, letterGrade));
        }

        // GPA Calculation
        double gpa = totalCreditHours > 0 ? totalQualityPoints / totalCreditHours : 0;

        reportContent.append("------------------------------------------------------\n")
                .append("Academic Summary:\n")
                .append("• Total Courses: ").append(totalCourses).append("\n")
                .append("• GPA: ").append(String.format("%.2f", gpa)).append("\n")
                .append("• Total Credits: ").append(totalCreditHours).append("\n")
                .append("• Status: Active\n")
                .append("------------------------------------------------------\n")
                .append("Date of Issue: ").append(java.time.LocalDate.now()).append("\n");

        // Save report to a file
        String fileName = System.getProperty("user.home") + "/Documents/Student_" + studentId + "_Report.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(reportContent.toString());
        }

        JOptionPane.showMessageDialog(null, "Report saved to: " + fileName);
    }
}

private String getLetterGrade(double percentage) {
    if (percentage >= 95) return "A+";
        if (percentage >= 90) return "A";
        if (percentage >= 85) return "B+";
        if (percentage >= 80) return "B";
        if (percentage >= 75) return "C+";
        if (percentage >= 70) return "C";
        if (percentage >= 65) return "D+";
        if (percentage >= 60) return "D";
        return "F";
}


// Helper Method to Map Final Grade Percentage to Grade Point
private double getGradePoint(double finalGrade) {
    if (finalGrade >= 95) return 5.0; // A
    if (finalGrade >= 90) return 4.51; // A
    if (finalGrade >= 85) return 4.01; // A-
    if (finalGrade >= 80) return 3.51; // B+
    if (finalGrade >= 75) return 3.01; // B
    if (finalGrade >= 70) return 2.51; // B-
    if (finalGrade >= 65) return 2.01; // C+
    if (finalGrade >= 60) return 1.01; // C
    return 0.0; // F
}


    private ArrayList<String[]> fetchAllStudents() throws SQLException {
        ArrayList<String[]> students = new ArrayList<>();
        String query = "SELECT student_id, name FROM Students";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                students.add(new String[]{String.valueOf(rs.getInt("student_id")), rs.getString("name")});
            }
        }

        return students;
    }

    private void enrollStudentToCourses() {
    try {
        ArrayList<String[]> students = fetchAllStudents();
        ArrayList<String[]> courses = fetchAllCourses();

        if (students.isEmpty() || courses.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Students or courses unavailable.");
            return;
        }

        JComboBox<String> studentComboBox = new JComboBox<>(students.stream().map(s -> s[1]).toArray(String[]::new));
        JList<String> courseList = new JList<>(courses.stream().map(c -> c[1]).toArray(String[]::new));
        courseList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Select Student:"), BorderLayout.NORTH);
        panel.add(studentComboBox, BorderLayout.CENTER);
        panel.add(new JScrollPane(courseList), BorderLayout.SOUTH);

        if (JOptionPane.showConfirmDialog(null, panel, "Enroll Student to Courses", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int studentId = Integer.parseInt(students.get(studentComboBox.getSelectedIndex())[0]);
            boolean enrolledAnyCourse = false;

            for (int courseIndex : courseList.getSelectedIndices()) {
                int courseId = Integer.parseInt(courses.get(courseIndex)[0]);
                if (assignStudentToCourse(studentId, courseId)) {
                    enrolledAnyCourse = true; // Mark that at least one enrollment occurred
                }
            }

            if (enrolledAnyCourse) {
                JOptionPane.showMessageDialog(null, "Student enrolled successfully.");
                loadStudentData(); // Refresh table
            } else {
                JOptionPane.showMessageDialog(null, "No new courses were enrolled. The student is already enrolled in the selected courses.");
            }
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error enrolling student: " + ex.getMessage());
    }
}


    private boolean assignStudentToCourse(int studentId, int courseId) throws SQLException {
    String checkQuery = "SELECT COUNT(*) FROM Enrollments WHERE student_id = ? AND course_id = ?";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {

        checkStmt.setInt(1, studentId);
        checkStmt.setInt(2, courseId);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next() && rs.getInt(1) > 0) {
            return false; // Student is already enrolled
        }
    }

    String query = "INSERT INTO Enrollments (student_id, course_id) VALUES (?, ?)";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setInt(1, studentId);
        stmt.setInt(2, courseId);
        stmt.executeUpdate();
        return true; // Enrollment successful
    }
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
    private boolean emailExists(String email) throws SQLException {
    String query = "SELECT COUNT(*) FROM Students WHERE email = ?";
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

    private void editStudent(JTable table) {
    int selectedRow = table.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Select a student to edit.", "Error", JOptionPane.ERROR_MESSAGE);
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

    if (JOptionPane.showConfirmDialog(null, fields, "Edit Student", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        String newName = nameField.getText().trim();
        String newEmail = emailField.getText().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Name and email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Check if the new email already exists for another student
            if (!newEmail.equals(currentEmail) && emailExists(newEmail)) {
                JOptionPane.showMessageDialog(null, "The email \"" + newEmail + "\" is already in use.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update the student's details in the database
            String query = "UPDATE Students SET name = ?, email = ? WHERE email = ?";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, newName);
                stmt.setString(2, newEmail);
                stmt.setString(3, currentEmail);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Student updated successfully.");
                loadStudentData();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error editing student: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}


    private void removeEnrollment(JTable table) {
    int selectedRow = table.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Select a student to remove enrollment.");
        return;
    }

    // Get the student details
    String studentName = (String) tableModel.getValueAt(selectedRow, 0);
    String enrolledCourses = (String) tableModel.getValueAt(selectedRow, 2);

    if (enrolledCourses == null || enrolledCourses.isEmpty() || enrolledCourses.equals("Not Enrolled")) {
        JOptionPane.showMessageDialog(null, "This student is not enrolled in any courses.");
        return;
    }

    // Split the courses to allow selection
    String[] courses = enrolledCourses.split(", ");
    JList<String> courseList = new JList<>(courses);
    courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel("Select a course to remove:"), BorderLayout.NORTH);
    panel.add(new JScrollPane(courseList), BorderLayout.CENTER);

    if (JOptionPane.showConfirmDialog(null, panel, "Remove Enrollment", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        String selectedCourse = courseList.getSelectedValue();

        if (selectedCourse == null || selectedCourse.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No course selected for removal.");
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            // Fetch the student ID
            String studentQuery = "SELECT student_id FROM Students WHERE name = ?";
            int studentId = -1;

            try (PreparedStatement studentStmt = connection.prepareStatement(studentQuery)) {
                studentStmt.setString(1, studentName);
                ResultSet rs = studentStmt.executeQuery();
                if (rs.next()) {
                    studentId = rs.getInt("student_id");
                } else {
                    JOptionPane.showMessageDialog(null, "Student not found.");
                    return;
                }
            }

            // Delete the enrollment
            String deleteQuery = "DELETE FROM Enrollments " +
                                 "WHERE student_id = ? AND course_id = (SELECT course_id FROM Courses WHERE name = ?)";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                deleteStmt.setInt(1, studentId);
                deleteStmt.setString(2, selectedCourse);

                int rowsAffected = deleteStmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Enrollment removed successfully.");
                    loadStudentData(); // Refresh the table to reflect changes
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to remove enrollment. Please ensure the course exists.");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error removing enrollment: " + ex.getMessage());
        }
    }
}



    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(30, 144, 255));
        button.addActionListener(listener);
        return button;
    }
}