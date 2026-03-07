package com.college.studentportal.service;

import org.springframework.stereotype.Service;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;

@Service
public class AIPredictionService {

    public double predictCGPA(double sgpa1, double sgpa2, double sgpa3, double sgpa4) throws Exception {

        // Define attributes (features)
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("sgpa1"));
        attributes.add(new Attribute("sgpa2"));
        attributes.add(new Attribute("sgpa3"));
        attributes.add(new Attribute("sgpa4"));
        attributes.add(new Attribute("finalCgpa"));

        // Create dataset structure
        Instances dataset = new Instances("StudentData", attributes, 0);
        dataset.setClassIndex(4);

        // Add some training data
        dataset.add(new DenseInstance(1.0,new double[]{6.5,6.8,7.0,7.2,7.0}));
        dataset.add(new DenseInstance(1.0,new double[]{7.0,7.2,7.5,7.7,7.4}));
        dataset.add(new DenseInstance(1.0,new double[]{7.5,7.8,8.0,8.2,7.9}));
        dataset.add(new DenseInstance(1.0,new double[]{8.0,8.2,8.4,8.5,8.3}));
        dataset.add(new DenseInstance(1.0,new double[]{6.8,7.0,7.1,7.3,7.05}));
        dataset.add(new DenseInstance(1.0,new double[]{7.2,7.4,7.6,7.8,7.5}));
        dataset.add(new DenseInstance(1.0,new double[]{7.8,8.0,8.2,8.4,8.1}));

        // Train model
        LinearRegression model = new LinearRegression();
        model.buildClassifier(dataset);

        // Create new student instance
        DenseInstance newStudent = new DenseInstance(5);
        newStudent.setValue(attributes.get(0), sgpa1);
        newStudent.setValue(attributes.get(1), sgpa2);
        newStudent.setValue(attributes.get(2), sgpa3);
        newStudent.setValue(attributes.get(3), sgpa4);
        newStudent.setDataset(dataset);

        // Predict CGPA
        double predicted = model.classifyInstance(newStudent);

        return Math.round(predicted * 100.0) / 100.0;
    }
}