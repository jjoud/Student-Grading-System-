package com.mycompany.projectgrading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class CourseManagementPage {
    private User currentUser;
    private DefaultTableModel tableModel;
    private JPanel mainPanel;

    public CourseManagementPage(User user) {
        this.currentUser = user;

        // Main Panel Setup
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Title Label
        JLabel titleLabel = new JLabel("Manage Courses", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Courses Table
        tableModel = new DefaultTableModel(new String[]{"Course Name", "Grading Weights"}, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton addButton = createButton("Add Course", e -> addCourse());
        JButton editButton = createButton("Edit Course", e -> editCourse(table));
        JButton deleteButton = createButton("Delete Course", e -> deleteCourse(table));
        JButton generateReportButton = createButton("Generate Report", e -> generateCourseReports());

        
        buttonPanel.add(generateReportButton);


        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load courses into the table
        loadCourseData();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    private void loadCourseData() {
        tableModel.setRowCount(0);
        String query = "SELECT name, CONCAT(assignment_weight, '% Assignment, ', quiz_weight, '% Quiz, ', exam_weight, '% Exam') AS weights FROM Courses";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("weights")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading courses: " + ex.getMessage());
        }
    }
private void generateCourseReports() {
    try {
        ArrayList<String[]> courses = fetchAllCourses(); // Fetch courses

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No courses available.");
            return;
        }

        // Allow admin to select one or multiple courses
        JList<String> courseList = new JList<>(courses.stream().map(c -> c[1]).toArray(String[]::new));
        courseList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Select Course(s):"), BorderLayout.NORTH);
        panel.add(new JScrollPane(courseList), BorderLayout.CENTER);

        if (JOptionPane.showConfirmDialog(null, panel, "Generate Course Reports", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            for (int index : courseList.getSelectedIndices()) {
                int courseId = Integer.parseInt(courses.get(index)[0]);
                String courseName = courses.get(index)[1];
                generateCourseReport(courseId, courseName);
            }

            JOptionPane.showMessageDialog(null, "Reports generated successfully.");
        }
    } catch (SQLException | IOException ex) {
        JOptionPane.showMessageDialog(null, "Error generating reports: " + ex.getMessage());
    }
}

private void generateCourseReport(int courseId, String courseName) throws SQLException, IOException {
    try (Connection connection = DatabaseConfig.getConnection()) {
        // Fetch course statistics
        String courseStatsQuery = """
            SELECT
                COUNT(DISTINCT a.assignment_id) AS sections,
                COUNT(DISTINCT e.student_id) AS total_students,
                AVG(g.assignment_score) AS avg_assignment,
                AVG(g.quiz_score) AS avg_quiz,
                AVG(g.exam_score) AS avg_exam,
                ROUND(AVG((g.assignment_score * c.assignment_weight / 100) +
                          (g.quiz_score * c.quiz_weight / 100) +
                          (g.exam_score * c.exam_weight / 100)), 2) AS avg_final_grade,
                SUM(CASE WHEN g.assignment_score >= 50 AND g.quiz_score >= 50 AND g.exam_score >= 50 THEN 1 ELSE 0 END) AS total_passed,
                SUM(CASE WHEN g.assignment_score < 50 OR g.quiz_score < 50 OR g.exam_score < 50 THEN 1 ELSE 0 END) AS total_failed
            FROM Courses c
            LEFT JOIN Assignments a ON c.course_id = a.course_id
            LEFT JOIN Grades g ON c.course_id = g.course_id
            LEFT JOIN Enrollments e ON g.student_id = e.student_id
            WHERE c.course_id = ?
        """;

        PreparedStatement statsStmt = connection.prepareStatement(courseStatsQuery);
        statsStmt.setInt(1, courseId);
        ResultSet statsRs = statsStmt.executeQuery();

        if (!statsRs.next()) {
            JOptionPane.showMessageDialog(null, "No data available for course: " + courseName);
            return;
        }

        // Collect statistics
        int sections = statsRs.getInt("sections");
        int totalStudents = statsRs.getInt("total_students");
        double avgAssignment = statsRs.getDouble("avg_assignment");
        double avgQuiz = statsRs.getDouble("avg_quiz");
        double avgExam = statsRs.getDouble("avg_exam");
        double avgFinalGrade = statsRs.getDouble("avg_final_grade");
        int totalPassed = statsRs.getInt("total_passed");
        int totalFailed = statsRs.getInt("total_failed");

        // Fetch class-level performance
        String performanceQuery = """
            SELECT
                c.name AS course_name,
                (SELECT u.name FROM Users u
                 JOIN Assignments a ON u.user_id = a.teacher_id
                 WHERE a.course_id = c.course_id LIMIT 1) AS instructor_name,
                AVG(g.assignment_score) AS avg_assignment,
                AVG(g.quiz_score) AS avg_quiz,
                AVG(g.exam_score) AS avg_exam,
                ROUND(AVG((g.assignment_score * c.assignment_weight / 100) +
                          (g.quiz_score * c.quiz_weight / 100) +
                          (g.exam_score * c.exam_weight / 100)), 2) AS final_grade
            FROM Grades g
            JOIN Courses c ON g.course_id = c.course_id
            WHERE c.course_id = ?
            GROUP BY c.course_id
        """;

        PreparedStatement performanceStmt = connection.prepareStatement(performanceQuery);
        performanceStmt.setInt(1, courseId);
        ResultSet performanceRs = performanceStmt.executeQuery();

        // Prepare report content
        StringBuilder reportContent = new StringBuilder();
        reportContent.append("------------------------------------------------------\n")
                     .append("                 COURSE PERFORMANCE REPORT\n")
                     .append("------------------------------------------------------\n")
                     .append("Course Name: ").append(courseName).append("\n")
                     .append("Number of Sections: ").append(sections).append("\n")
                     .append("Total Students: ").append(totalStudents).append("\n")
                     .append("\n------------------------------------------------------\n")
                     .append("Overall Statistics:\n")
                     .append("• Average Assignment Score: ").append(String.format("%.2f%%", avgAssignment)).append("\n")
                     .append("• Average Quiz Score: ").append(String.format("%.2f%%", avgQuiz)).append("\n")
                     .append("• Average Exam Score: ").append(String.format("%.2f%%", avgExam)).append("\n")
                     .append("• Overall Average Final Grade: ").append(String.format("%.2f%%", avgFinalGrade)).append("\n")
                     .append("• Pass/Fail Ratio: ").append(totalPassed).append(" Passed / ").append(totalFailed).append(" Failed\n")
                     .append("\n------------------------------------------------------\n")
                     .append("Class-Level Performance:\n")
                     .append("------------------------------------------------------\n")
                     .append("| Course Name  | Instructor   | Avg Assignment | Avg Quiz | Avg Exam | Final Grade |\n")
                     .append("|--------------|--------------|----------------|----------|----------|-------------|\n");

        while (performanceRs.next()) {
            String instructorName = performanceRs.getString("instructor_name") != null ? performanceRs.getString("instructor_name") : "Unassigned";
            double avgAssgn = performanceRs.getDouble("avg_assignment");
            double avgQz = performanceRs.getDouble("avg_quiz");
            double avgExm = performanceRs.getDouble("avg_exam");
            double finalGrd = performanceRs.getDouble("final_grade");

            reportContent.append(String.format("| %-12s | %-12s | %-14.2f | %-8.2f | %-8.2f | %-11.2f |\n",
                    courseName, instructorName, avgAssgn, avgQz, avgExm, finalGrd));
        }

        reportContent.append("------------------------------------------------------\n")
                     .append("Date of Issue: ").append(java.time.LocalDate.now()).append("\n");

        // Save the report as a TXT file
        String fileName = System.getProperty("user.home") + "/Documents/" + courseName.replace(" ", "_") + "_Performance_Report.txt";
        try (FileWriter writer = new FileWriter(new File(fileName))) {
            writer.write(reportContent.toString());
        }

        JOptionPane.showMessageDialog(null, "Report generated for course: " + courseName + "\nSaved at: " + fileName);
    }
}
    

    private void addCourse() {
    JTextField courseNameField = new JTextField();
    JTextField assignmentWeightField = new JTextField();
    JTextField quizWeightField = new JTextField();
    JTextField examWeightField = new JTextField();
    JTextField creditHoursField = new JTextField();

    Object[] fields = {
        "Course Name:", courseNameField,
        "Assignment Weight (%):", assignmentWeightField,
        "Quiz Weight (%):", quizWeightField,
        "Exam Weight (%):", examWeightField,
        "Credit Hours:", creditHoursField
    };

    if (JOptionPane.showConfirmDialog(null, fields, "Add Course", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
            // Validate that all fields are filled
            if (courseNameField.getText().trim().isEmpty() ||
                assignmentWeightField.getText().trim().isEmpty() ||
                quizWeightField.getText().trim().isEmpty() ||
                examWeightField.getText().trim().isEmpty() ||
                creditHoursField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "All fields are required.");
                return;
            }

            String courseName = courseNameField.getText().trim();
            int assignmentWeight = Integer.parseInt(assignmentWeightField.getText().trim());
            int quizWeight = Integer.parseInt(quizWeightField.getText().trim());
            int examWeight = Integer.parseInt(examWeightField.getText().trim());
            int creditHours = Integer.parseInt(creditHoursField.getText().trim());

            // Validate that each weight is between 0 and 100
            if (assignmentWeight < 0 || assignmentWeight > 100 || 
                quizWeight < 0 || quizWeight > 100 || 
                examWeight < 0 || examWeight > 100) {
                JOptionPane.showMessageDialog(null, "Each weight must be between 0 and 100.");
                return;
            }

            if (assignmentWeight + quizWeight + examWeight != 100) {
                JOptionPane.showMessageDialog(null, "The total weight must equal 100%.");
                return;
            }

            String query = "INSERT INTO Courses (name, assignment_weight, quiz_weight, exam_weight, credit_hours) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, courseName);
                stmt.setInt(2, assignmentWeight);
                stmt.setInt(3, quizWeight);
                stmt.setInt(4, examWeight);
                stmt.setInt(5, creditHours);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "Course added successfully.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error adding course: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter valid numbers for weights and credit hours.");
        }
    }
}

    
    private void editCourse(JTable table) {
    int selectedRow = table.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Please select a course to edit.");
        return;
    }

    String courseName = (String) tableModel.getValueAt(selectedRow, 0);

    JTextField assignmentWeightField = new JTextField();
    JTextField quizWeightField = new JTextField();
    JTextField examWeightField = new JTextField();

    Object[] fields = {
        "Assignment Weight (%):", assignmentWeightField,
        "Quiz Weight (%):", quizWeightField,
        "Exam Weight (%):", examWeightField
    };

    if (JOptionPane.showConfirmDialog(null, fields, "Edit Course", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
            int assignmentWeight = Integer.parseInt(assignmentWeightField.getText().trim());
            int quizWeight = Integer.parseInt(quizWeightField.getText().trim());
            int examWeight = Integer.parseInt(examWeightField.getText().trim());

            // Validate that each weight is between 0 and 100
            if (assignmentWeight < 0 || assignmentWeight > 100 || 
                quizWeight < 0 || quizWeight > 100 || 
                examWeight < 0 || examWeight > 100) {
                JOptionPane.showMessageDialog(null, "Each weight must be between 0 and 100.");
                return;
            }

            if (assignmentWeight + quizWeight + examWeight != 100) {
                JOptionPane.showMessageDialog(null, "The total weight must equal 100%.");
                return;
            }

            String query = "UPDATE Courses SET assignment_weight = ?, quiz_weight = ?, exam_weight = ? WHERE name = ?";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, assignmentWeight);
                stmt.setInt(2, quizWeight);
                stmt.setInt(3, examWeight);
                stmt.setString(4, courseName);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Course updated successfully.");
                loadCourseData();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error updating course: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter valid numbers for weights.");
        }
    }
}

    




    private void deleteCourse(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a course to delete.");
            return;
        }

        String courseName = (String) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the course: " + courseName + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM Courses WHERE name = ?";
            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, courseName);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Course deleted successfully.");
                loadCourseData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error deleting course: " + ex.getMessage());
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
    private ArrayList<String[]> fetchAllCourses() throws SQLException {
    ArrayList<String[]> courses = new ArrayList<>();
    String query = "SELECT course_id, name FROM Courses";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            courses.add(new String[]{
                String.valueOf(rs.getInt("course_id")),
                rs.getString("name")
            });
        }
    } catch (SQLException ex) {
        System.err.println("Error fetching courses: " + ex.getMessage());
        throw ex; // Re-throw the exception for higher-level handling
    }
    return courses;
}

}