package com.mycompany.projectgrading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

class TeacherDashboard extends JFrame {
    private DefaultTableModel tableModel;
    private JTable courseTable; // Table to display courses and weights
    private ArrayList<Course> courses; // List of courses assigned to the teacher
    private User currentUser; // Logged-in teacher

    public TeacherDashboard(User currentUser) {
        this.currentUser = currentUser;

        // Set up the JFrame
        setTitle("Teacher Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left Panel: Contains table and buttons
        JPanel leftPanel = createLeftPanel();

        // Right Panel: Contains navigation buttons
        JPanel rightPanel = createRightPanel();

        // Add panels to the main frame
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Load courses assigned to the teacher and populate the table
        loadTeacherCourses();
        updateCourseTable();
    }

    /**
     * Creates the left panel with a table and an Edit Weights button.
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(600, 600));

        // Table: Display courses assigned to the teacher
        tableModel = new DefaultTableModel(new String[]{"Course Name", "Assignment Weight", "Quiz Weight", "Exam Weight"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };
        courseTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(courseTable);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel: Contains Edit Weights button
        JPanel buttonPanel = new JPanel();
        JButton buttonEditWeights = new JButton("Edit Weights");
        buttonEditWeights.addActionListener(e -> openEditWeightsDialog());
        buttonPanel.add(buttonEditWeights);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    /**
     * Creates the right panel with navigation buttons.
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(173, 216, 230));
        rightPanel.setLayout(null);
        rightPanel.setPreferredSize(new Dimension(200, 600));

        // Navigation Buttons
        JButton buttonCourseWeights = createTextButton("Course Management", 50, e -> updateCourseTable()); // Default page
        JButton buttonGradeInputSystem = createTextButton("Grade Input System", 100, e -> {
            new GradeInputPage(courses, currentUser).setVisible(true); // Open Grade Input Page
            dispose(); // Close TeacherDashboard
        });

        JButton buttonLogout = createTextButton("Logout", 500, e -> {
            new LoginPage().setVisible(true);
            dispose();
        });

        // Add buttons to the right panel
        rightPanel.add(buttonCourseWeights);
        rightPanel.add(buttonGradeInputSystem);
        rightPanel.add(buttonLogout);

        return rightPanel;
    }

    /**
     * Loads courses assigned to the teacher from the database.
     */
    private void loadTeacherCourses() {
        courses = new ArrayList<>();
        String query = "SELECT c.course_id, c.name, c.assignment_weight, c.quiz_weight, c.exam_weight " +
                       "FROM Courses c " +
                       "JOIN Assignments a ON c.course_id = a.course_id " +
                       "WHERE a.teacher_id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, currentUser.getId()); // Get teacher ID
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(new Course(
                    rs.getInt("course_id"),
                    rs.getString("name"),
                    rs.getInt("assignment_weight"),
                    rs.getInt("quiz_weight"),
                    rs.getInt("exam_weight")
                ));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading courses: " + ex.getMessage());
        }
    }

    /**
     * Updates the course table with the current courses and weights.
     */
    private void updateCourseTable() {
        tableModel.setRowCount(0); // Clear existing rows
        for (Course course : courses) {
            tableModel.addRow(new Object[]{
                course.getName(),
                course.getAssignmentWeight() + "%",
                course.getQuizWeight() + "%",
                course.getExamWeight() + "%"
            });
        }
    }
    private void validateWeights(int assignmentWeight, int quizWeight, int examWeight) throws IllegalArgumentException {
    if (assignmentWeight < 0 || assignmentWeight > 100 ||
        quizWeight < 0 || quizWeight > 100 ||
        examWeight < 0 || examWeight > 100) {
        throw new IllegalArgumentException("All weights must be between 0 and 100.");
    }
    if (assignmentWeight + quizWeight + examWeight != 100) {
        throw new IllegalArgumentException("The total weight must equal 100%.");
    }
}

    /**
     * Opens a dialog to edit weights for the selected course.
     */
    private void openEditWeightsDialog() {
    int selectedRow = courseTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a course to edit weights.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    Course selectedCourse = courses.get(selectedRow);

    JTextField assignmentWeightField = new JTextField(String.valueOf(selectedCourse.getAssignmentWeight()));
    JTextField quizWeightField = new JTextField(String.valueOf(selectedCourse.getQuizWeight()));
    JTextField examWeightField = new JTextField(String.valueOf(selectedCourse.getExamWeight()));

    Object[] fields = {
        "Assignment Weight (%):", assignmentWeightField,
        "Quiz Weight (%):", quizWeightField,
        "Exam Weight (%):", examWeightField
    };

    if (JOptionPane.showConfirmDialog(this, fields, "Edit Weights", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
            int assignmentWeight = Integer.parseInt(assignmentWeightField.getText().trim());
            int quizWeight = Integer.parseInt(quizWeightField.getText().trim());
            int examWeight = Integer.parseInt(examWeightField.getText().trim());

            // Validate weights
            validateWeights(assignmentWeight, quizWeight, examWeight);

            // Update weights in the database
            updateCourseWeights(selectedCourse.getCourseId(), assignmentWeight, quizWeight, examWeight);

            // Update weights locally and refresh the table
            selectedCourse.setAssignmentWeight(assignmentWeight);
            selectedCourse.setQuizWeight(quizWeight);
            selectedCourse.setExamWeight(examWeight);
            updateCourseTable();

            JOptionPane.showMessageDialog(this, "Weights updated successfully.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for the weights.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

    /**
     * Updates course weights in the database.
     */
    private void updateCourseWeights(int courseId, int assignmentWeight, int quizWeight, int examWeight) {
        String query = "UPDATE Courses SET assignment_weight = ?, quiz_weight = ?, exam_weight = ? WHERE course_id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, assignmentWeight);
            stmt.setInt(2, quizWeight);
            stmt.setInt(3, examWeight);
            stmt.setInt(4, courseId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating weights: " + ex.getMessage());
        }
    }

    /**
     * Creates a text-only button for navigation.
     */
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
}