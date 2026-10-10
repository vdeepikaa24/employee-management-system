package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Department;
import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.dto.EmployeeRequest;
import com.example.employeemanagementsystem.dto.EmployeeResponse;
import com.example.employeemanagementsystem.projection.EmployeeSummary;
import com.example.employeemanagementsystem.projection.EmployeeView;
import com.example.employeemanagementsystem.repository.DepartmentRepository;
import com.example.employeemanagementsystem.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmployeeService {

    private static final int BATCH_SIZE = 25;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return toResponse(getEmployee(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findByDepartmentId(Long departmentId) {
        return employeeRepository.findEmployeesByDepartmentId(departmentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findByDepartmentName(String departmentName) {
        return employeeRepository.findUsingNamedQuery(departmentName)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummary> findProjectionByDepartmentId(Long departmentId) {
        return employeeRepository.findProjectedByDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> findEmployeeViewsByDepartmentId(Long departmentId) {
        return employeeRepository.findEmployeeViewsByDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchByName(String name, Pageable pageable) {
        return employeeRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setDepartment(getDepartment(request.departmentId()));
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = getEmployee(id);
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setDepartment(getDepartment(request.departmentId()));
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public int saveBatch(List<EmployeeRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            EmployeeRequest request = requests.get(i);
            Employee employee = new Employee();
            employee.setName(request.name());
            employee.setEmail(request.email());
            employee.setDepartment(departmentRepository.getReferenceById(
                    request.departmentId()));
            entityManager.persist(employee);

            if ((i + 1) % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        return requests.size();
    }

    @Transactional
    public void deleteById(Long id) {
        employeeRepository.delete(getEmployee(id));
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findWithDepartmentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }

    private Department getDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + id));
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getCreatedBy(),
                employee.getCreatedDate(),
                employee.getLastModifiedBy(),
                employee.getLastModifiedDate());
    }
}
