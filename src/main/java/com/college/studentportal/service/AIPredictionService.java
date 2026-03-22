package com.college.studentportal.service;

import com.college.studentportal.model.Result;
import com.college.studentportal.model.Student;
import com.college.studentportal.repository.ResultRepository;
import com.college.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIPredictionService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;

    public AIPredictionService(ResultRepository resultRepository, StudentRepository studentRepository) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
    }

    private Instances buildStudentDataset(int numFeatures, String className) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 1; i <= numFeatures; i++) {
            attributes.add(new Attribute("sgpa" + i));
        }
        attributes.add(new Attribute(className));
        Instances dataset = new Instances("StudentData", attributes, 0);
        dataset.setClassIndex(numFeatures);
        return dataset;
    }

    private void addHardcodedTrainingData(Instances dataset) {
        dataset.add(new DenseInstance(1.0, new double[]{6.5, 6.8, 7.0, 7.2, 7.0}));
        dataset.add(new DenseInstance(1.0, new double[]{7.0, 7.2, 7.5, 7.7, 7.4}));
        dataset.add(new DenseInstance(1.0, new double[]{7.5, 7.8, 8.0, 8.2, 7.9}));
        dataset.add(new DenseInstance(1.0, new double[]{8.0, 8.2, 8.4, 8.5, 8.3}));
        dataset.add(new DenseInstance(1.0, new double[]{6.8, 7.0, 7.1, 7.3, 7.05}));
        dataset.add(new DenseInstance(1.0, new double[]{7.2, 7.4, 7.6, 7.8, 7.5}));
        dataset.add(new DenseInstance(1.0, new double[]{7.8, 8.0, 8.2, 8.4, 8.1}));
        dataset.add(new DenseInstance(1.0, new double[]{8.5, 8.8, 9.0, 9.2, 8.9}));
        dataset.add(new DenseInstance(1.0, new double[]{9.0, 9.2, 9.3, 9.5, 9.25}));
        dataset.add(new DenseInstance(1.0, new double[]{6.0, 6.2, 6.5, 6.8, 6.4}));
    }

    private void populateDynamicTrainingData(Instances dataset, int requiredSemesters, boolean predictingCGPA) {
        List<Student> students = studentRepository.findAll();
        int addedCount = 0;

        for (Student student : students) {
            List<Result> results = resultRepository.findByStudentId(student.getId());
            if (results.isEmpty()) continue;

            Map<Integer, List<Result>> semesterResults = new HashMap<>();
            double totalWeightedScore = 0;
            int totalCredits = 0;

            for (Result r : results) {
                int sem = r.getSemester();
                semesterResults.computeIfAbsent(sem, k -> new ArrayList<>()).add(r);
                totalWeightedScore += r.getGradePoint() * r.getCredits();
                totalCredits += r.getCredits();
            }

            if (semesterResults.size() >= requiredSemesters) {
                double[] features = new double[requiredSemesters + 1];
                for (int i = 1; i <= requiredSemesters; i++) {
                    List<Result> semRes = semesterResults.get(i);
                    if (semRes == null) {
                        features[i - 1] = 7.0; // fallback avg
                    } else {
                        double semScore = 0;
                        int semCreds = 0;
                        for (Result r : semRes) {
                            semScore += r.getGradePoint() * r.getCredits();
                            semCreds += r.getCredits();
                        }
                        features[i - 1] = semCreds == 0 ? 0 : semScore / semCreds;
                    }
                }

                if (predictingCGPA) {
                    features[requiredSemesters] = totalCredits == 0 ? 0 : totalWeightedScore / totalCredits;
                } else {
                    List<Result> nextSemRes = semesterResults.get(requiredSemesters + 1);
                    if (nextSemRes != null) {
                        double nextScore = 0;
                        int nextCreds = 0;
                        for (Result r : nextSemRes) {
                            nextScore += r.getGradePoint() * r.getCredits();
                            nextCreds += r.getCredits();
                        }
                        features[requiredSemesters] = nextCreds == 0 ? 0 : nextScore / nextCreds;
                    } else {
                        features[requiredSemesters] = features[requiredSemesters - 1]; // fallback to last
                    }
                }

                dataset.add(new DenseInstance(1.0, features));
                addedCount++;
            }
        }

        if (addedCount < 5 && requiredSemesters == 4 && predictingCGPA) {
            addHardcodedTrainingData(dataset);
        }
    }

    public double predictCGPA(double sgpa1, double sgpa2, double sgpa3, double sgpa4) throws Exception {
        Instances dataset = buildStudentDataset(4, "finalCgpa");
        populateDynamicTrainingData(dataset, 4, true);

        RandomForest model = new RandomForest();
        model.setNumIterations(100);
        model.buildClassifier(dataset);

        DenseInstance newStudent = new DenseInstance(5);
        newStudent.setDataset(dataset);
        newStudent.setValue(0, sgpa1);
        newStudent.setValue(1, sgpa2);
        newStudent.setValue(2, sgpa3);
        newStudent.setValue(3, sgpa4);

        double predicted = model.classifyInstance(newStudent);
        return Math.round(predicted * 100.0) / 100.0;
    }

    public double predictNextSGPA(List<Double> sgpas) throws Exception {
        int numFeatures = sgpas.size();
        if (numFeatures == 0) return 7.0;

        Instances dataset = buildStudentDataset(numFeatures, "nextSgpa");
        populateDynamicTrainingData(dataset, numFeatures, false);

        if (dataset.numInstances() < 5) {
            for (int i = 0; i < 10; i++) {
                double[] synthetic = new double[numFeatures + 1];
                double base = 6.0 + (i * 0.3);
                for (int j = 0; j < numFeatures; j++) {
                    synthetic[j] = base + (j * 0.1);
                }
                synthetic[numFeatures] = base + (numFeatures * 0.1);
                dataset.add(new DenseInstance(1.0, synthetic));
            }
        }

        RandomForest model = new RandomForest();
        model.setNumIterations(50);
        model.buildClassifier(dataset);

        DenseInstance instance = new DenseInstance(numFeatures + 1);
        instance.setDataset(dataset);
        for (int i = 0; i < numFeatures; i++) {
            instance.setValue(i, sgpas.get(i));
        }

        double prediction = model.classifyInstance(instance);
        return Math.round(prediction * 100.0) / 100.0;
    }
}