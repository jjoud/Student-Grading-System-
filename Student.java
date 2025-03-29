package com.mycompany.projectgrading;

public class Student {
    private String name;
    private int assignmentScore;
    private int quizScore;
    private int examScore;

    public Student(String name, int assignmentScore, int quizScore, int examScore) {
        this.name = name;
        this.assignmentScore = assignmentScore;
        this.quizScore = quizScore;
        this.examScore = examScore;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAssignmentScore() {
        return assignmentScore;
    }

    public void setAssignmentScore(int assignmentScore) {
        this.assignmentScore = assignmentScore;
    }

    public int getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(int quizScore) {
        this.quizScore = quizScore;
    }

    public int getExamScore() {
        return examScore;
    }

    public void setExamScore(int examScore) {
        this.examScore = examScore;
    }

    // Method to calculate the final grade with course-specific weights
    public int calculateFinalGrade(int assignmentWeight, int quizWeight, int examWeight) {
        return (int) (assignmentScore * (assignmentWeight / 100.0) +
                      quizScore * (quizWeight / 100.0) +
                      examScore * (examWeight / 100.0));
    }

    // Method to convert a percentage grade to a letter grade
    public String convertToLetterGrade(int percentage) {
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
}