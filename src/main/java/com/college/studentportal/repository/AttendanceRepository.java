package com.college.studentportal.repository;

import com.college.studentportal.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<Attendance> findBySubjectIdAndDate(Long subjectId, LocalDate date);

    Optional<Attendance> findByStudentIdAndSubjectIdAndDate(Long studentId, Long subjectId, LocalDate date);
}
