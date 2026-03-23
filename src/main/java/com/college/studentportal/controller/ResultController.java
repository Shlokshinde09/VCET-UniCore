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

    public ResultController(ResultRepository resultRepository,
                            StudentRepository studentRepository) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping
    public Result addResult(@RequestBody java.util.Map<String, Object> payload) {
        
        java.util.Map<String, Object> studentMap = (java.util.Map<String, Object>) payload.get("student");
        Long studentId = Long.parseLong(studentMap.get("id").toString());
        
        Student student = studentRepository.findById(studentId).orElseThrow();

        Result result = new Result();
        result.setStudent(student);
        result.setCourseCode(String.valueOf(payload.get("courseCode")));
        result.setCourseName(String.valueOf(payload.get("courseName")));
        result.setInternalMarks(Double.parseDouble(payload.get("internalMarks").toString()));
        result.setExternalMarks(Double.parseDouble(payload.get("externalMarks").toString()));
        result.setCredits(Integer.parseInt(payload.get("credits").toString()));
        result.setSemester(Integer.parseInt(payload.get("semester").toString()));

        // MU NEP Calculation Logic
        double total = result.getInternalMarks() + result.getExternalMarks();
        result.setTotalMarks(total);

        String grade = "F";
        double point = 0.0;

        if (total >= 90) { grade = "O"; point = 10.0; }
        else if (total >= 80) { grade = "A+"; point = 9.0; }
        else if (total >= 70) { grade = "A"; point = 8.0; }
        else if (total >= 60) { grade = "B+"; point = 7.0; }
        else if (total >= 50) { grade = "B"; point = 6.0; }
        else if (total >= 45) { grade = "C"; point = 5.0; }
        else if (total >= 40) { grade = "P"; point = 4.0; }

        result.setGrade(grade);
        result.setGradePoint(point);
        result.setCreditGrade(point * result.getCredits());

        return resultRepository.save(result);
    }

    // 🔹 Get results of a student
    @GetMapping("/student/{studentId}")
    public List<Result> getResultsByStudent(@PathVariable("studentId") Long studentId) {
        return resultRepository.findByStudentId(studentId);
    }

    // 🔹 Get ALL results
    @GetMapping
    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    // 🔹 Delete a result by id
    @DeleteMapping("/{id}")
    public void deleteResult(@PathVariable("id") Long id) {
        resultRepository.deleteById(id);
    }

    // 🔹 Calculate CGPA
    @GetMapping("/cgpa/{studentId}")
    public double calculateCGPA(@PathVariable("studentId") Long studentId) {
        List<Result> results = resultRepository.findByStudentId(studentId);
        double totalCreditGrades = 0;
        int totalCredits = 0;

        for (Result r : results) {
            totalCreditGrades += r.getCreditGrade();
            totalCredits += r.getCredits();
        }

        if (totalCredits == 0) return 0;
        return Math.round((totalCreditGrades / totalCredits) * 100.0) / 100.0;
    }

    // 🔹 Calculate SGPA (semester-wise)
    @GetMapping("/sgpa/{studentId}/{semester}")
    public double calculateSGPA(@PathVariable("studentId") Long studentId, @PathVariable("semester") int semester) {
        List<Result> results = resultRepository.findByStudentIdAndSemester(studentId, semester);
        double totalCreditGrades = 0;
        int totalCredits = 0;

        for (Result r : results) {
            totalCreditGrades += r.getCreditGrade();
            totalCredits += r.getCredits();
        }

        if (totalCredits == 0) return 0;
        return Math.round((totalCreditGrades / totalCredits) * 100.0) / 100.0;
    }
}