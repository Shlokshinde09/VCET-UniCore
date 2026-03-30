package com.college.studentportal.controller;

import com.college.studentportal.model.Student;
import com.college.studentportal.repository.StudentRepository;
import com.college.studentportal.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final AuthService authService;

    public StudentController(StudentRepository studentRepository, AuthService authService) {
        this.studentRepository = studentRepository;
        this.authService = authService;
    }

    /**
     * Admin creates a new student.
     * Account is created in an "unclaimed" state (password remains null).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        student.setPassword("");
        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        student.setClaimToken(token);
        Student saved = studentRepository.save(student);

        return ResponseEntity.ok(Map.of(
                "student", saved,
                "claimToken", token,
                "message",
                "Student created successfully. The account is unclaimed. The student must use the Account Claim portal with the Claim Code to set their password."));
    }

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
     * Admin resets a student's password by nullifying it, forcing another 'Account
     * Claim'.
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setPassword("");
                    String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    student.setClaimToken(token);
                    studentRepository.save(student);
                    return ResponseEntity.ok(Map.of(
                            "claimToken", (Object) token,
                            "message", (Object) ("Password reset successfully. New Claim Code: " + token)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Student changes their own password.
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password cannot be empty."));
        }
        return studentRepository.findById(id)
                .map(student -> {
                    student.setPassword(authService.hashPassword(newPassword));
                    studentRepository.save(student);
                    return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentRepository.deleteById(id);
    }
}