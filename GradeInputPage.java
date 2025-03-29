package com.mycompany.projectgrading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

class GradeInputPage extends JFrame {
    private ArrayList<Course> courses;
    private DefaultTableModel tableModel;
    private JComboBox<String> courseSelector;
    private JComboBox<String> gradingScaleSelector;
    private JTable tableStudents;
    private User currentUser;

    public GradeInputPage(ArrayList<Course> courses, User currentUser) {
        this.courses = courses;
        this.currentUser = currentUser;

        // Set up JFrame
        setTitle("Grade Input System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left Panel: Contains the main content
        JPanel leftPanel = createLeftPanel();

        // Right Panel: Shared navigation buttons
        JPanel rightPanel = createNavigationPanel();

        // Add panels to the main frame
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Populate the table for the selected course
        if (!courses.isEmpty()) {
            updateStudentTable(courses.get(0).getName());
        }
    }

    /**
     * Creates the left panel with selectors, table, and action buttons.
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(null);
        leftPanel.setPreferredSize(new Dimension(600, 600));

        // Title Label
        JLabel labelTitle = new JLabel("Grade Input System:");
        labelTitle.setFont(new Font("Arial", Font.BOLD, 18));
        labelTitle.setBounds(50, 20, 300, 30);
        leftPanel.add(labelTitle);

        // Course Selector
        JLabel labelCourse = new JLabel("Select Course:");
        labelCourse.setFont(new Font("Arial", Font.PLAIN, 14));
        labelCourse.setBounds(50, 70, 100, 30);
        leftPanel.add(labelCourse);

        courseSelector = new JComboBox<>(courses.stream().map(Course::getName).toArray(String[]::new));
        courseSelector.setBounds(150, 70, 150, 30);
        courseSelector.addActionListener(e -> updateStudentTable((String) courseSelector.getSelectedItem()));
        leftPanel.add(courseSelector);

        // Grading Scale Selector
        JLabel labelGradingScale = new JLabel("Grading Scale:");
        labelGradingScale.setFont(new Font("Arial", Font.PLAIN, 14));
        labelGradingScale.setBounds(350, 70, 100, 30);
        leftPanel.add(labelGradingScale);

        gradingScaleSelector = new JComboBox<>(new String[]{"Percentage", "Letter"});
        gradingScaleSelector.setBounds(450, 70, 100, 30);
        gradingScaleSelector.addActionListener(e -> updateStudentTable((String) courseSelector.getSelectedItem()));
        leftPanel.add(gradingScaleSelector);

        // Students Table
        tableModel = new DefaultTableModel(
                new String[]{"Student ID", "Student Name", "Assignment", "Quiz", "Exam", "Final Grade"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };
        tableStudents = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tableStudents);
        scrollPane.setBounds(50, 120, 500, 300);
        leftPanel.add(scrollPane);

        // Action Buttons
        leftPanel.add(createStyledButton("Add Grade", 50, 450, e -> addGrade((String) courseSelector.getSelectedItem())));
        leftPanel.add(createStyledButton("Edit Grade", 220, 450, e -> editGrade((String) courseSelector.getSelectedItem())));

        return leftPanel;
    }

    /**
     * Creates the shared navigation panel with buttons for different pages.
     */
    private JPanel createNavigationPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(173, 216, 230));
        rightPanel.setLayout(null);
        rightPanel.setPreferredSize(new Dimension(200, 600));

        JButton buttonCourseManagement = createTextButton("Course Management", 50, e -> {
            new TeacherDashboard(currentUser).setVisible(true);
            dispose();
        });

        JButton buttonGradeInputSystem = createTextButton("Grade Input System", 100, e -> {
            JOptionPane.showMessageDialog(this, "You are already in the Grade Input System!");
        });

        JButton buttonLogout = createTextButton("Logout", 500, e -> {
            new LoginPage().setVisible(true);
            dispose();
        });

        rightPanel.add(buttonCourseManagement);
        rightPanel.add(buttonGradeInputSystem);
        rightPanel.add(buttonLogout);

        return rightPanel;
    }

    private JButton createStyledButton(String text, int x, int y, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBounds(x, y, 150, 30);
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(30, 144, 255));
        button.addActionListener(listener);
        return button;
    }

    private JButton createTextButton(String text, int yPosition, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setForeground(Color.BLACK);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBounds(20, yPosition, 160, 30);
        button.addActionListener(listener);
        return button;
    }

    private void updateStudentTable(String courseName) {
        tableModel.setRowCount(0);

        String query = "SELECT s.student_id, s.name, g.assignment_score, g.quiz_score, g.exam_score " +
                "FROM Students s " +
                "LEFT JOIN Grades g ON s.student_id = g.student_id " +
                "AND g.course_id = (SELECT course_id FROM Courses WHERE name = ?) " +
                "WHERE s.student_id IN (SELECT student_id FROM Enrollments " +
                "WHERE course_id = (SELECT course_id FROM Courses WHERE name = ?))";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, courseName);
            stmt.setString(2, courseName);

            ResultSet rs = stmt.executeQuery();
            int[] weights = getCourseWeights(courseName);
            int assignmentWeight = weights[0];
            int quizWeight = weights[1];
            int examWeight = weights[2];

            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                String studentName = rs.getString("name");
                int assignmentScore = rs.getInt("assignment_score");
                int quizScore = rs.getInt("quiz_score");
                int examScore = rs.getInt("exam_score");

                double finalGrade = (assignmentScore * assignmentWeight / 100.0) +
                        (quizScore * quizWeight / 100.0) +
                        (examScore * examWeight / 100.0);

                String gradeDisplay = gradingScaleSelector.getSelectedItem().equals("Percentage")
                        ? String.format("%.2f%%", finalGrade)
                        : convertToLetterGrade(finalGrade);

                tableModel.addRow(new Object[]{
                        studentId, studentName,
                        assignmentScore > 0 ? assignmentScore : "Not Entered",
                        quizScore > 0 ? quizScore : "Not Entered",
                        examScore > 0 ? examScore : "Not Entered",
                        gradeDisplay
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching student grades: " + ex.getMessage());
        }
    }

    private void addGrade(String courseName) {
    int selectedRow = tableStudents.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a student to add grades.");
        return;
    }

    int studentId = (int) tableModel.getValueAt(selectedRow, 0);

    try (Connection connection = DatabaseConfig.getConnection()) {
        // Check if the grade already exists
        String checkGradeQuery = "SELECT COUNT(*) FROM Grades WHERE student_id = ? AND course_id = (SELECT course_id FROM Courses WHERE name = ?)";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkGradeQuery)) {
            checkStmt.setInt(1, studentId);
            checkStmt.setString(2, courseName);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Grade already exists for this student. Please use the Edit option to update the grade.");
                return;
            }
        }

        // Proceed with adding a new grade
        JTextField assignmentScoreField = new JTextField();
        JTextField quizScoreField = new JTextField();
        JTextField examScoreField = new JTextField();

        Object[] gradeInputFields = {
            "Assignment Score:", assignmentScoreField,
            "Quiz Score:", quizScoreField,
            "Exam Score:", examScoreField
        };

        if (JOptionPane.showConfirmDialog(this, gradeInputFields, "Add Grades", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int assignmentScore = Integer.parseInt(assignmentScoreField.getText().trim());
                int quizScore = Integer.parseInt(quizScoreField.getText().trim());
                int examScore = Integer.parseInt(examScoreField.getText().trim());

                if (assignmentScore < 0 || assignmentScore > 100 || 
                    quizScore < 0 || quizScore > 100 || 
                    examScore < 0 || examScore > 100) {
                    JOptionPane.showMessageDialog(this, "Scores must be between 0 and 100.");
                    return;
                }

                String insertGradeQuery = "INSERT INTO Grades (student_id, course_id, assignment_score, quiz_score, exam_score) " +
                                          "VALUES (?, (SELECT course_id FROM Courses WHERE name = ?), ?, ?, ?) " +
                                          "ON DUPLICATE KEY UPDATE assignment_score = ?, quiz_score = ?, exam_score = ?";
                try (PreparedStatement stmt = connection.prepareStatement(insertGradeQuery)) {
                    stmt.setInt(1, studentId);
                    stmt.setString(2, courseName);
                    stmt.setInt(3, assignmentScore);
                    stmt.setInt(4, quizScore);
                    stmt.setInt(5, examScore);
                    stmt.setInt(6, assignmentScore);
                    stmt.setInt(7, quizScore);
                    stmt.setInt(8, examScore);
                    stmt.executeUpdate();

                    // Add a notification for the student
                    String notificationQuery = "INSERT INTO Notifications (student_id, message) VALUES (?, ?)";
                    try (PreparedStatement notificationStmt = connection.prepareStatement(notificationQuery)) {
                        notificationStmt.setInt(1, studentId);
                        notificationStmt.setString(2, courseName + " grade is added.");
                        notificationStmt.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "Grade added successfully.");
                    updateStudentTable(courseName); // Refresh the table
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for the scores.");
            }
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error adding grades: " + ex.getMessage());
    }
}



private void editGrade(String courseName) {
    int selectedRow = tableStudents.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a student to edit grades.");
        return;
    }

    int studentId = (int) tableModel.getValueAt(selectedRow, 0);
    JTextField assignmentScoreField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
    JTextField quizScoreField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 3)));
    JTextField examScoreField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 4)));

    Object[] gradeInputFields = {
        "Assignment Score:", assignmentScoreField,
        "Quiz Score:", quizScoreField,
        "Exam Score:", examScoreField
    };

    if (JOptionPane.showConfirmDialog(this, gradeInputFields, "Edit Grades", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
            int assignmentScore = Integer.parseInt(assignmentScoreField.getText().trim());
            int quizScore = Integer.parseInt(quizScoreField.getText().trim());
            int examScore = Integer.parseInt(examScoreField.getText().trim());

            if (assignmentScore < 0 || assignmentScore > 100 || 
                quizScore < 0 || quizScore > 100 || 
                examScore < 0 || examScore > 100) {
                JOptionPane.showMessageDialog(this, "Scores must be between 0 and 100.");
                return;
            }

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                     "UPDATE Grades SET assignment_score = ?, quiz_score = ?, exam_score = ? " +
                     "WHERE student_id = ? AND course_id = (SELECT course_id FROM Courses WHERE name = ?)")) {

                stmt.setInt(1, assignmentScore);
                stmt.setInt(2, quizScore);
                stmt.setInt(3, examScore);
                stmt.setInt(4, studentId);
                stmt.setString(5, courseName);
                stmt.executeUpdate();

                // Add a notification for the student
                String notificationQuery = "INSERT INTO Notifications (student_id, message) VALUES (?, ?)";
                try (PreparedStatement notificationStmt = connection.prepareStatement(notificationQuery)) {
                    notificationStmt.setInt(1, studentId);
                    notificationStmt.setString(2, courseName + " grade is edited.");
                    notificationStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Grade updated successfully.");
                updateStudentTable(courseName); // Refresh the table
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for the scores.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating grades: " + ex.getMessage());
        }
    }
}



    

    private int[] getCourseWeights(String courseName) {
    String query = "SELECT assignment_weight, quiz_weight, exam_weight FROM Courses WHERE name = ?";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setString(1, courseName); // Set the course name parameter
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int assignmentWeight = rs.getInt("assignment_weight");
            int quizWeight = rs.getInt("quiz_weight");
            int examWeight = rs.getInt("exam_weight");

            // Debugging output
            System.out.println("Fetched Weights - Assignment: " + assignmentWeight + "%, Quiz: " + quizWeight + "%, Exam: " + examWeight + "%");

            return new int[]{assignmentWeight, quizWeight, examWeight};
        } else {
            JOptionPane.showMessageDialog(this, "No course found with name: " + courseName);
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error fetching course weights: " + ex.getMessage());
        ex.printStackTrace();
    }

    // Return default weights if fetching fails
    return new int[]{0, 0, 0};
}


    private String convertToLetterGrade(double finalGrade) {
        if (finalGrade >= 95) return "A+";
        if (finalGrade >= 90) return "A";
        if (finalGrade >= 85) return "B+";
        if (finalGrade >= 80) return "B";
        if (finalGrade >= 75) return "C+";
        if (finalGrade >= 70) return "C";
        if (finalGrade >= 65) return "D+";
        if (finalGrade >= 60) return "D";
        return "F";
    }
}