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
import java.util.ArrayList;
import java.util.Objects;

@Service
public class EmployeeService {

    private static final int BATCH_SIZE = 25;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeHistoryService historyService;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           EmployeeHistoryService historyService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.historyService = historyService;
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

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchByNameAndDepartment(
            String name, Long departmentId, Pageable pageable) {
        Page<Employee> employees = departmentId == null
                ? employeeRepository.findByNameContainingIgnoreCase(name, pageable)
                : employeeRepository.findByNameContainingIgnoreCaseAndDepartmentId(
                        name, departmentId, pageable);
        return employees
                .map(this::toResponse);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.name());
        employee.setEmail(request.email());
        setJobDetails(employee, request);
        employee.setDepartment(getDepartment(request.departmentId()));
        Employee saved = employeeRepository.saveAndFlush(employee);
        historyService.record(saved.getId(), saved.getName(), "CREATED",
                "Employee added to " + saved.getDepartment().getName() + ".");
        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = getEmployee(id);
        List<String> changes = new ArrayList<>();
        addChange(changes, "Name", employee.getName(), request.name());
        addChange(changes, "Email", employee.getEmail(), request.email());
        Department newDepartment = getDepartment(request.departmentId());
        addChange(changes, "Department", employee.getDepartment().getName(), newDepartment.getName());
        addChange(changes, "Job title", employee.getJobTitle(), normalizeOptionalText(request.jobTitle()));
        addChange(changes, "Phone number", employee.getPhoneNumber(), normalizeOptionalText(request.phoneNumber()));
        addChange(changes, "Hire date", employee.getHireDate(), request.hireDate());
        employee.setName(request.name());
        employee.setEmail(request.email());
        setJobDetails(employee, request);
        employee.setDepartment(newDepartment);
        Employee saved = employeeRepository.saveAndFlush(employee);
        if (!changes.isEmpty()) {
            historyService.record(saved.getId(), saved.getName(), "UPDATED", String.join("; ", changes) + ".");
        }
        return toResponse(saved);
    }

    @Transactional
    public int saveBatch(List<EmployeeRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            EmployeeRequest request = requests.get(i);
            Employee employee = new Employee();
            employee.setName(request.name());
            employee.setEmail(request.email());
            setJobDetails(employee, request);
            employee.setDepartment(departmentRepository.getReferenceById(
                    request.departmentId()));
            entityManager.persist(employee);
            historyService.record(employee.getId(), employee.getName(), "CREATED",
                    "Employee imported into " + employee.getDepartment().getName() + ".");

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
        Employee employee = getEmployee(id);
        historyService.record(employee.getId(), employee.getName(), "DELETED", "Employee record deleted.");
        employeeRepository.delete(employee);
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

    private void setJobDetails(Employee employee, EmployeeRequest request) {
        employee.setJobTitle(normalizeOptionalText(request.jobTitle()));
        employee.setPhoneNumber(normalizeOptionalText(request.phoneNumber()));
        employee.setHireDate(request.hireDate());
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void addChange(List<String> changes, String label, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(label + " changed from " + displayValue(oldValue)
                    + " to " + displayValue(newValue));
        }
    }

    private String displayValue(Object value) {
        return value == null || value.toString().isBlank() ? "(empty)" : value.toString();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getJobTitle(),
                employee.getPhoneNumber(),
                employee.getHireDate(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getCreatedBy(),
                employee.getCreatedDate(),
                employee.getLastModifiedBy(),
                employee.getLastModifiedDate());
    }
}
