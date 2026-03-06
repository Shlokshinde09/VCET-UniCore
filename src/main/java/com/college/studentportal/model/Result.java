package com.college.studentportal.model;

import jakarta.persistence.*;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double gradePoint;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public Result() {}

    public Result(double gradePoint, Student student, Subject subject) {
        this.gradePoint = gradePoint;
        this.student = student;
        this.subject = subject;
    }

    public Long getId() { return id; }

    public double getGradePoint() { return gradePoint; }
    public void setGradePoint(double gradePoint) { this.gradePoint = gradePoint; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
}