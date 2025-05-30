package com.mycompany.projectgrading;
import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {
    private User currentUser;

    public AdminDashboard(User user) {
        this.currentUser = user;

        // Set up JFrame
        setTitle("Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left Panel: Default page (Course Management)
        JPanel leftPanel = new CourseManagementPage(currentUser).getPanel();

        // Right Panel: Navigation buttons
        JPanel rightPanel = createRightPanel();

        // Add panels to JFrame
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(173, 216, 230));
        rightPanel.setLayout(null);
        rightPanel.setPreferredSize(new Dimension(200, 600));

        JButton buttonCourseManagement = createTextButton("Course Management", 50, e -> {
            getContentPane().removeAll();
            add(new CourseManagementPage(currentUser).getPanel(), BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
            revalidate();
            repaint();
        });

        JButton buttonEnrollStudent = createTextButton("Student Page", 100, e -> {
            getContentPane().removeAll();
            add(new StudentManagementPage(currentUser).getPanel(), BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
            revalidate();
            repaint();
        });

        JButton buttonAssignTeacher = createTextButton("Teacher Page", 150, e -> {
            getContentPane().removeAll();
            add(new TeacherManagementPage(currentUser).getPanel(), BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
            revalidate();
            repaint();
        });

        JButton buttonLogout = createTextButton("Logout", 500, e -> {
            new LoginPage().setVisible(true);
            dispose();
        });

        rightPanel.add(buttonCourseManagement);
        rightPanel.add(buttonEnrollStudent);
        rightPanel.add(buttonAssignTeacher);
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
}