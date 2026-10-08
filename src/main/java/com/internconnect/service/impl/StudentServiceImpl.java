package com.internconnect.service.impl;

import com.internconnect.entity.Student;
import com.internconnect.exception.ResourceNotFoundException;
import com.internconnect.repository.StudentRepository;
import com.internconnect.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Student updateStudent(Long id, Student updatedStudent) {
        Student existing = getStudentById(id);
        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setPhone(updatedStudent.getPhone());
        existing.setAddress(updatedStudent.getAddress());
        existing.setCollege(updatedStudent.getCollege());
        existing.setBranch(updatedStudent.getBranch());
        existing.setYear(updatedStudent.getYear());
        existing.setCgpa(updatedStudent.getCgpa());
        existing.setSkills(updatedStudent.getSkills());
        existing.setResumeUrl(updatedStudent.getResumeUrl());
        existing.setProfilePictureUrl(updatedStudent.getProfilePictureUrl());
        return studentRepository.save(existing);
    }

    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student", "id", id);
        }
        studentRepository.deleteById(id);
    }
}
