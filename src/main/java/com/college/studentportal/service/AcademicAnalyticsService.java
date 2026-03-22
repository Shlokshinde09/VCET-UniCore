package com.college.studentportal.service;

import com.college.studentportal.model.Result;
import com.college.studentportal.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AcademicAnalyticsService {

    private final ResultRepository resultRepository;

    public AcademicAnalyticsService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public double calculateCGPA(Long studentId){

        List<Result> results = resultRepository.findByStudentId(studentId);

        double totalWeightedScore = 0;
        int totalCredits = 0;

        for(Result r : results){
            totalWeightedScore += r.getGradePoint()*r.getCredits();
            totalCredits += r.getCredits();
        }

        if(totalCredits == 0) return 0;

        return Math.round((totalWeightedScore/totalCredits)*100.0)/100.0;
    }

    public Map<Integer,Double> calculateSemesterSGPA(Long studentId){

        List<Result> results = resultRepository.findByStudentId(studentId);

        Map<Integer,List<Result>> semesterResults = new HashMap<>();

        for(Result r : results){

            int sem = r.getSemester();

            semesterResults
                    .computeIfAbsent(sem,k->new ArrayList<>())
                    .add(r);
        }

        Map<Integer,Double> semesterSgpa = new TreeMap<>();

        for(Map.Entry<Integer,List<Result>> entry : semesterResults.entrySet()){

            double weightedScore = 0;
            int credits = 0;

            for(Result r : entry.getValue()){
                weightedScore += r.getGradePoint()*r.getCredits();
                credits += r.getCredits();
            }

            double sgpa = credits==0?0:weightedScore/credits;

            semesterSgpa.put(entry.getKey(),
                    Math.round(sgpa*100.0)/100.0);
        }

        return semesterSgpa;
    }

    public Map<String, List<String>> getSubjectInsights(Long studentId) {
        List<Result> results = resultRepository.findByStudentId(studentId);
        
        List<String> strongSubjects = new ArrayList<>();
        List<String> weakSubjects = new ArrayList<>();

        for (Result r : results) {
            if (r.getGradePoint() >= 8.5) {
                strongSubjects.add(r.getCourseName());
            } else if (r.getGradePoint() < 7.0) {
                weakSubjects.add(r.getCourseName());
            }
        }

        Map<String, List<String>> insights = new HashMap<>();
        insights.put("strong", strongSubjects);
        insights.put("weak", weakSubjects);
        
        return insights;
    }

}