package com.college.studentportal.controller;

import com.college.studentportal.model.Student;
import com.college.studentportal.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Admin creates a new student. A default password is auto-generated
     * after save (vcet@<id>) and returned in the response so admin can
     * distribute it to the student.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        // First save without password to get the auto-generated id
        Student saved = studentRepository.save(student);

        // Generate default password based on the student's id
        String defaultPassword = "vcet@" + saved.getId();
        saved.setPassword(defaultPassword);
        studentRepository.save(saved);

        // Return student info + the generated password (one-time visibility for admin)
        return ResponseEntity.ok(Map.of(
                "student", saved,
                "generatedPassword", defaultPassword,
                "message", "Student created successfully. Default password: " + defaultPassword
        ));
    }

    // Get All Students
    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student incoming) {
        return studentRepository.findById(id)
                .map(existing -> {
                    existing.setName(incoming.getName());
                    existing.setEmail(incoming.getEmail());
                    existing.setDepartment(incoming.getDepartment());
                    existing.setSemester(incoming.getSemester());
                    return ResponseEntity.ok(studentRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Admin resets a student's password back to the default (vcet@<id>).
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    String defaultPassword = "vcet@" + student.getId();
                    student.setPassword(defaultPassword);
                    studentRepository.save(student);
                    return ResponseEntity.ok(Map.of(
                            "message", "Password reset successful. New password: " + defaultPassword,
                            "newPassword", defaultPassword
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Student changes their own password.
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password cannot be empty."));
        }
        return studentRepository.findById(id)
                .map(student -> {
                    student.setPassword(newPassword);
                    studentRepository.save(student);
                    return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete Student
    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentRepository.deleteById(id);
    }
}