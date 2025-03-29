package com.mycompany.projectgrading;
import java.util.ArrayList;


public class Course {
    private int courseId; // Assigned by the database
    private String name;
    private int assignmentWeight;
    private int quizWeight;
    private int examWeight;

    // List to hold students enrolled in this course
    private ArrayList<Student> students;

    // Constructor without courseId
    public Course(String name, int assignmentWeight, int quizWeight, int examWeight) {
        this.name = name;
        this.assignmentWeight = assignmentWeight;
        this.quizWeight = quizWeight;
        this.examWeight = examWeight;
        this.students = new ArrayList<>(); // Initialize the students list
    }

    // Overloaded constructor (e.g., for fetching from the database)
    public Course(int courseId, String name, int assignmentWeight, int quizWeight, int examWeight) {
        this.courseId = courseId;
        this.name = name;
        this.assignmentWeight = assignmentWeight;
        this.quizWeight = quizWeight;
        this.examWeight = examWeight;
        this.students = new ArrayList<>();
    }

    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAssignmentWeight() {
        return assignmentWeight;
    }

    public void setAssignmentWeight(int assignmentWeight) {
        this.assignmentWeight = assignmentWeight;
    }

    public int getQuizWeight() {
        return quizWeight;
    }

    public void setQuizWeight(int quizWeight) {
        this.quizWeight = quizWeight;
    }

    public int getExamWeight() {
        return examWeight;
    }

    public void setExamWeight(int examWeight) {
        this.examWeight = examWeight;
    }

    // Methods for managing students
    public ArrayList<Student> getStudents() {
        return students;
    }

    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }

    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
        }
    }

    public void removeStudent(Student student) {
        students.remove(student);
    }

    // Helper method for debugging
    @Override
    public String toString() {
        return "Course{" +
                "courseId=" + courseId +
                ", name='" + name + '\'' +
                ", assignmentWeight=" + assignmentWeight +
                ", quizWeight=" + quizWeight +
                ", examWeight=" + examWeight +
                ", students=" + students.size() +
                '}';
    }
}