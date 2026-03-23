package com.college.studentportal.controller;

import com.college.studentportal.model.Student;
import com.college.studentportal.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentRepository studentRepository;

    public AuthController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Student login — verifies email AND password.
     * Returns the student object (without password) on success, or 401 on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("email") String email, @RequestParam("password") String password) {

        Optional<Student> studentOpt = studentRepository.findByEmail(email);

        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }

        Student student = studentOpt.get();

        // Verify password (plain-text comparison — acceptable for a college project)
        if (student.getPassword() == null || !student.getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }

        // Success — return student data (password excluded via @JsonIgnore)
        return ResponseEntity.ok(student);
    }

    /**
     * Forgot-password flow — no automated email reset.
     * Returns instructions for contacting admin/IT office.
     */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestParam("email") String email) {
        if (email == null || email.isBlank()) {
            return Map.of("message", "Please enter your registered email address.");
        }
        return Map.of(
                "message",
                "Your login credentials were provided by the college administration when your account was created. "
                        + "If you've forgotten your password, please contact your class coordinator or visit the IT Help Desk "
                        + "with your college ID card to get your password reset. "
                        + "Administrators: contact the IT department directly for credential recovery."
        );
    }
}