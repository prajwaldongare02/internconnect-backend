package com.internconnect.service;

import com.internconnect.entity.Student;

import java.util.List;

public interface StudentService {
    Student getStudentById(Long id);
    Student getStudentByEmail(String email);
    List<Student> getAllStudents();
    Student updateStudent(Long id, Student student);
    void deleteStudent(Long id);
}
