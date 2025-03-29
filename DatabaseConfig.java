package com.mycompany.projectgrading;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL = "jdbc:mysql://localhost:3306/student_grading_system"; 
    private static final String USERNAME = "root"; 
    private static final String PASSWORD = "1234"; 

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}