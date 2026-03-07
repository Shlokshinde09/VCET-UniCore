package com.college.studentportal.controller;

import com.college.studentportal.model.Student;
import com.college.studentportal.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentRepository studentRepository;

    public AuthController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PostMapping("/login")
    public Student login(@RequestParam String email) {

        Optional<Student> student = studentRepository.findByEmail(email);

        return student.orElse(null);
    }
}