package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.projection.EmployeeSummary;
import com.example.employeemanagementsystem.projection.EmployeeView;
import com.example.employeemanagementsystem.repository.EmployeeRepository;
import com.example.employeemanagementsystem.repository.DepartmentRepository;
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

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found: " + id
                ));
    }

    public List<Employee> findByDepartmentId(Long departmentId) {
        return employeeRepository.findEmployeesByDepartmentId(departmentId);
    }

    public List<Employee> findByDepartmentName(String departmentName) {
        return employeeRepository.findUsingNamedQuery(departmentName);
    }

    public List<EmployeeSummary> findProjectionByDepartmentId(Long departmentId) {
        return employeeRepository.findProjectedByDepartmentId(departmentId);
    }

    public List<EmployeeView> findEmployeeViewsByDepartmentId(Long departmentId) {
        return employeeRepository.findEmployeeViewsByDepartmentId(departmentId);
    }

    public Page<Employee> searchByName(String name, Pageable pageable) {
        return employeeRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public int saveBatch(List<Employee> employees) {
        final int batchSize = 25;

        for (int i = 0; i < employees.size(); i++) {
            Employee employee = employees.get(i);
            if (employee.getDepartment() == null
                    || employee.getDepartment().getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each employee must include a department id"
                );
            }

            employee.setDepartment(departmentRepository.getReferenceById(
                    employee.getDepartment().getId()));
            entityManager.persist(employee);

            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        return employees.size();
    }

    public void deleteById(Long id) {
        employeeRepository.deleteById(id);
    }
}
