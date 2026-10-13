package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Department;
import com.example.employeemanagementsystem.dto.DepartmentRequest;
import com.example.employeemanagementsystem.dto.DepartmentResponse;
import com.example.employeemanagementsystem.repository.DepartmentRepository;
import com.example.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<DepartmentResponse> findByNameUsingNamedQuery(String name) {
        return departmentRepository.findUsingNamedQuery(name).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<DepartmentResponse> findWithEmployeesByNameUsingNamedQuery(String name) {
        return departmentRepository.findWithEmployeesUsingNamedQuery(name)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return departmentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.name());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + id));
        department.setName(request.name());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void deleteById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + id));
        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete department '" + department.getName() + "': "
                            + employeeCount + " employee(s) are assigned to it.");
        }
        departmentRepository.delete(department);
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCreatedBy(),
                department.getCreatedDate(),
                department.getLastModifiedBy(),
                department.getLastModifiedDate());
    }
}
