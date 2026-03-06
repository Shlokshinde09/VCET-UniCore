package com.college.studentportal.controller;

import com.college.studentportal.model.*;
import com.college.studentportal.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/results")
public class ResultController {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public ResultController(ResultRepository resultRepository,
                            StudentRepository studentRepository,
                            SubjectRepository subjectRepository) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    // 🔹 Add result (marks)
    @PostMapping
    public Result addResult(@RequestParam Long studentId,
                            @RequestParam Long subjectId,
                            @RequestParam double gradePoint) {

        Student student = studentRepository.findById(studentId).orElseThrow();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();

        Result result = new Result();
        result.setStudent(student);
        result.setSubject(subject);
        result.setGradePoint(gradePoint);

        return resultRepository.save(result);
    }

    // 🔹 Get results of a student
    @GetMapping("/student/{studentId}")
    public List<Result> getResultsByStudent(@PathVariable Long studentId) {
        return resultRepository.findByStudentId(studentId);
    }

    // 🔹 Calculate CGPA
    @GetMapping("/cgpa/{studentId}")
    public double calculateCGPA(@PathVariable Long studentId) {

        List<Result> results = resultRepository.findByStudentId(studentId);

        double totalWeightedScore = 0;
        int totalCredits = 0;

        for (Result result : results) {
            double grade = result.getGradePoint();
            int credits = result.getSubject().getCredits();

            totalWeightedScore += grade * credits;
            totalCredits += credits;
        }

        if (totalCredits == 0) return 0;

        return Math.round((totalWeightedScore / totalCredits) * 100.0) / 100.0;
    }

    // 🔹 Calculate SGPA (semester-wise)
    @GetMapping("/sgpa/{studentId}/{semester}")
    public double calculateSGPA(@PathVariable Long studentId,
                                @PathVariable int semester) {

        List<Result> results =
                resultRepository.findByStudentIdAndSubjectSemester(studentId, semester);

        double totalWeightedScore = 0;
        int totalCredits = 0;

        for (Result result : results) {
            double grade = result.getGradePoint();
            int credits = result.getSubject().getCredits();

            totalWeightedScore += grade * credits;
            totalCredits += credits;
        }

        if (totalCredits == 0) return 0;

        return Math.round((totalWeightedScore / totalCredits) * 100.0) / 100.0;
    }
}