package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Department;
import com.example.employeemanagementsystem.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findByNameUsingNamedQuery(String name) {
        return departmentRepository.findUsingNamedQuery(name);
    }

    public Optional<Department> findWithEmployeesByNameUsingNamedQuery(String name) {
        return departmentRepository.findWithEmployeesUsingNamedQuery(name);
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Department not found: " + id));
    }

    public Department save(Department department) {
        return departmentRepository.save(department);
    }

    public void deleteById(Long id) {
        departmentRepository.deleteById(id);
    }
}
