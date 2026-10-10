package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ArchiveService {

    private final JdbcTemplate archiveJdbcTemplate;
    private final EmployeeRepository employeeRepository;

    public ArchiveService(
            @Qualifier("archiveJdbcTemplate") JdbcTemplate archiveJdbcTemplate,
            EmployeeRepository employeeRepository) {
        this.archiveJdbcTemplate = archiveJdbcTemplate;
        this.employeeRepository = employeeRepository;
    }

    @PostConstruct
    public void initializeArchiveTable() {
        archiveJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS archived_employees (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    department_name VARCHAR(255) NOT NULL,
                    archived_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
    }

    public Map<String, Object> archiveEmployee(Long id) {
        Employee employee = employeeRepository.findWithDepartmentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found: " + id
                ));

        archiveJdbcTemplate.update("""
                        INSERT INTO archived_employees
                            (id, name, email, department_name, archived_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment().getName());

        return Map.of("archived", true, "employeeId", id);
    }

    public List<Map<String, Object>> findArchivedEmployees() {
        return archiveJdbcTemplate.queryForList("""
                SELECT id, name, email, department_name, archived_at
                FROM archived_employees
                ORDER BY archived_at DESC
                """);
    }
}
