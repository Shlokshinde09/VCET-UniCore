package com.college.studentportal.controller;

import com.college.studentportal.model.Attendance;
import com.college.studentportal.model.Student;
import com.college.studentportal.model.Subject;
import com.college.studentportal.repository.AttendanceRepository;
import com.college.studentportal.repository.StudentRepository;
import com.college.studentportal.repository.SubjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public AttendanceController(AttendanceRepository attendanceRepository,
                                StudentRepository studentRepository,
                                SubjectRepository subjectRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    /**
     * Mark attendance for a single student+subject+date.
     */
    @PostMapping
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> payload) {
        Long studentId = Long.parseLong(payload.get("studentId").toString());
        Long subjectId = Long.parseLong(payload.get("subjectId").toString());
        LocalDate date = LocalDate.parse(payload.get("date").toString());
        String status = payload.get("status").toString().toUpperCase();

        Student student = studentRepository.findById(studentId).orElse(null);
        Subject subject = subjectRepository.findById(subjectId).orElse(null);

        if (student == null || subject == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid student or subject ID."));
        }

        // Update if already exists, otherwise create new
        Attendance attendance = attendanceRepository
                .findByStudentIdAndSubjectIdAndDate(studentId, subjectId, date)
                .orElse(new Attendance());

        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(date);
        attendance.setStatus(status);

        attendanceRepository.save(attendance);
        return ResponseEntity.ok(Map.of("message", "Attendance marked successfully."));
    }

    /**
     * Bulk mark attendance for all students of a subject on a given date.
     * Body: { "subjectId": 1, "date": "2026-03-26", "records": [{"studentId":1,"status":"PRESENT"}, ...] }
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkMarkAttendance(@RequestBody Map<String, Object> payload) {
        Long subjectId = Long.parseLong(payload.get("subjectId").toString());
        LocalDate date = LocalDate.parse(payload.get("date").toString());
        Subject subject = subjectRepository.findById(subjectId).orElse(null);

        if (subject == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid subject ID."));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) payload.get("records");

        int saved = 0;
        for (Map<String, Object> record : records) {
            Long studentId = Long.parseLong(record.get("studentId").toString());
            String status = record.get("status").toString().toUpperCase();

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;

            Attendance attendance = attendanceRepository
                    .findByStudentIdAndSubjectIdAndDate(studentId, subjectId, date)
                    .orElse(new Attendance());

            attendance.setStudent(student);
            attendance.setSubject(subject);
            attendance.setDate(date);
            attendance.setStatus(status);

            attendanceRepository.save(attendance);
            saved++;
        }

        return ResponseEntity.ok(Map.of("message", saved + " attendance records saved successfully."));
    }

    /**
     * Get all attendance records for a student.
     */
    @GetMapping("/student/{studentId}")
    public List<Attendance> getByStudent(@PathVariable Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    /**
     * Get attendance summary (per-subject percentage) for a student.
     */
    @GetMapping("/student/{studentId}/summary")
    public List<Map<String, Object>> getStudentSummary(@PathVariable Long studentId) {
        List<Attendance> records = attendanceRepository.findByStudentId(studentId);

        // Group by subject
        Map<Long, List<Attendance>> bySubject = records.stream()
                .collect(Collectors.groupingBy(a -> a.getSubject().getId()));

        List<Map<String, Object>> summaries = new ArrayList<>();

        for (Map.Entry<Long, List<Attendance>> entry : bySubject.entrySet()) {
            List<Attendance> subjectRecords = entry.getValue();
            String subjectName = subjectRecords.get(0).getSubject().getSubjectName();
            long total = subjectRecords.size();
            long present = subjectRecords.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
            long absent = total - present;
            double percentage = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("subjectId", entry.getKey());
            summary.put("subjectName", subjectName);
            summary.put("totalClasses", total);
            summary.put("present", present);
            summary.put("absent", absent);
            summary.put("percentage", percentage);
            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Get attendance for a specific subject on a specific date (admin pre-fill).
     */
    @GetMapping("/subject/{subjectId}/date/{date}")
    public List<Attendance> getBySubjectAndDate(@PathVariable Long subjectId,
                                                 @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return attendanceRepository.findBySubjectIdAndDate(subjectId, localDate);
    }

    /**
     * Delete an attendance record.
     */
    @DeleteMapping("/{id}")
    public void deleteAttendance(@PathVariable Long id) {
        attendanceRepository.deleteById(id);
    }
}
