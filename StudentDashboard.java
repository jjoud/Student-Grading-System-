
package com.mycompany.projectgrading;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class StudentDashboard extends JFrame {
    private User currentUser;

    public StudentDashboard(User currentUser) {
        this.currentUser = currentUser;

        // Set up JFrame
        setTitle("Student Dashboard - " + currentUser.getName());
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create UI components
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        // Add panels to the main frame
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }
    
    private JPanel createLeftPanel() {
    JPanel leftPanel = new JPanel();
    leftPanel.setBackground(Color.WHITE);
    leftPanel.setLayout(new BorderLayout());
    leftPanel.setPreferredSize(new Dimension(600, 600));

    JLabel labelTitle = new JLabel("Your Courses and Grades:");
    labelTitle.setFont(new Font("Arial", Font.BOLD, 18));
    labelTitle.setHorizontalAlignment(SwingConstants.CENTER);

    JTable tableCourses = new JTable();
    JScrollPane scrollPane = new JScrollPane(tableCourses);

    leftPanel.add(labelTitle, BorderLayout.NORTH);
    leftPanel.add(scrollPane, BorderLayout.CENTER);

    // Fetch and populate courses and grades
    populateCoursesAndGrades(tableCourses);

    return leftPanel;
}
private void populateCoursesAndGrades(JTable tableCourses) {
    DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"Course Name", "Assignment", "Quiz", "Exam", "Final Grade"}, 0);
    tableCourses.setModel(tableModel);

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(
             "SELECT c.name AS course_name, g.assignment_score, g.quiz_score, g.exam_score, " +
             "       (g.assignment_score * c.assignment_weight / 100 + " +
             "        g.quiz_score * c.quiz_weight / 100 + " +
             "        g.exam_score * c.exam_weight / 100) AS final_score " +
             "FROM Enrollments e " +
             "JOIN Courses c ON e.course_id = c.course_id " +
             "JOIN Grades g ON e.student_id = g.student_id AND e.course_id = g.course_id " +
             "WHERE e.student_id = ?")) {

        stmt.setInt(1, currentUser.getId()); // Use currentUser ID
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String courseName = rs.getString("course_name");
            int assignmentScore = rs.getInt("assignment_score");
            int quizScore = rs.getInt("quiz_score");
            int examScore = rs.getInt("exam_score");
            int finalScore = rs.getInt("final_score");

            // Create a Student instance to use convertToLetterGrade
            Student student = new Student(currentUser.getName(), assignmentScore, quizScore, examScore);
            String letterGrade = student.convertToLetterGrade(finalScore);

            tableModel.addRow(new Object[]{courseName, assignmentScore, quizScore, examScore, letterGrade});
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error fetching courses and grades: " + ex.getMessage());
    }
}

private JPanel createRightPanel() {
    JPanel rightPanel = new JPanel();
    rightPanel.setBackground(new Color(173, 216, 230));
    rightPanel.setLayout(null);
    rightPanel.setPreferredSize(new Dimension(200, 600));

    // Fetch notifications for the current user
    JTextArea notificationsArea = new JTextArea();
    notificationsArea.setEditable(false);
    notificationsArea.setLineWrap(true);
    notificationsArea.setWrapStyleWord(true);
    notificationsArea.setBackground(new Color(240, 248, 255));
    notificationsArea.setBounds(10, 10, 180, 480); // Space for notifications
    rightPanel.add(notificationsArea);

    // Populate notifications
    loadNotifications(notificationsArea);

    // Logout button
    JButton buttonLogout = createTextButton("Logout", 500, e -> {
        new LoginPage().setVisible(true);
        dispose();
    });

    rightPanel.add(buttonLogout);
    return rightPanel;
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
private JPanel createNotificationPanel(int studentId) {
    JPanel notificationPanel = new JPanel();
    notificationPanel.setBackground(new Color(173, 216, 230));
    notificationPanel.setLayout(new BoxLayout(notificationPanel, BoxLayout.Y_AXIS));

    JLabel title = new JLabel("Notifications:");
    title.setFont(new Font("Arial", Font.BOLD, 16));
    title.setAlignmentX(Component.CENTER_ALIGNMENT);
    notificationPanel.add(title);

    // Fetch notifications from the database
    String query = "SELECT message FROM Notifications WHERE student_id = ? ORDER BY created_at DESC LIMIT 10";
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setInt(1, studentId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            JLabel notification = new JLabel("- " + rs.getString("message"));
            notification.setFont(new Font("Arial", Font.PLAIN, 14));
            notification.setAlignmentX(Component.LEFT_ALIGNMENT);
            notificationPanel.add(notification);
        }
    } catch (SQLException ex) {
        JLabel error = new JLabel("Error loading notifications.");
        error.setForeground(Color.RED);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        notificationPanel.add(error);
    }

    return notificationPanel;
}
private void loadNotifications(JTextArea notificationsArea) {
    String query = "SELECT message FROM Notifications WHERE student_id = ?";
    StringBuilder notifications = new StringBuilder();

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement stmt = connection.prepareStatement(query)) {

        stmt.setInt(1, currentUser.getId()); // Fetch notifications for the logged-in student
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            notifications.append("- ").append(rs.getString("message")).append("\n");
        }

        if (notifications.length() == 0) {
            notifications.append("No notifications.");
        }

        notificationsArea.setText(notifications.toString());
    } catch (SQLException ex) {
        notificationsArea.setText("Error loading notifications: " + ex.getMessage());
    }
}


}