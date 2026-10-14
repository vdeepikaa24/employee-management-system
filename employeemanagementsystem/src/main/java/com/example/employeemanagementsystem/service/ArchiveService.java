package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.Department;
import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.repository.DepartmentRepository;
import com.example.employeemanagementsystem.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ArchiveService {

    private final JdbcTemplate archiveJdbcTemplate;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public ArchiveService(
            @Qualifier("archiveJdbcTemplate") JdbcTemplate archiveJdbcTemplate,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository) {
        this.archiveJdbcTemplate = archiveJdbcTemplate;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @PostConstruct
    public void initializeArchiveTable() {
        archiveJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS archived_employees (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    department_id BIGINT,
                    department_name VARCHAR(255) NOT NULL,
                    archived_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        // Adds the department id when opening an archive database created by an older version.
        archiveJdbcTemplate.execute(
                "ALTER TABLE archived_employees ADD COLUMN IF NOT EXISTS department_id BIGINT");
    }

    @Transactional
    public Map<String, Object> archiveEmployee(Long id) {
        Employee employee = employeeRepository.findWithDepartmentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found: " + id
                ));

        try {
            employeeRepository.delete(employee);
            employeeRepository.flush();
            archiveJdbcTemplate.update("""
                            INSERT INTO archived_employees
                                (id, name, email, department_id, department_name, archived_at)
                            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                            """,
                    employee.getId(),
                    employee.getName(),
                    employee.getEmail(),
                    employee.getDepartment().getId(),
                    employee.getDepartment().getName());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This employee already has an archived record.");
        }

        return Map.of("archived", true, "employeeId", id);
    }

    @Transactional
    public Map<String, Object> restoreEmployee(Long id) {
        ArchiveSnapshot snapshot = archiveJdbcTemplate.query("""
                        SELECT id, name, email, department_id, department_name
                        FROM archived_employees
                        WHERE id = ?
                        """,
                (resultSet, rowNumber) -> new ArchiveSnapshot(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getObject("department_id", Long.class),
                        resultSet.getString("department_name")),
                id).stream().findFirst().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Archived employee not found: " + id));

        if (employeeRepository.findByEmail(snapshot.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active employee already uses this email address.");
        }

        Department department = findDepartment(snapshot);
        Employee employee = new Employee();
        employee.setName(snapshot.name());
        employee.setEmail(snapshot.email());
        employee.setDepartment(department);
        Employee restored = employeeRepository.saveAndFlush(employee);

        archiveJdbcTemplate.update(
                "DELETE FROM archived_employees WHERE id = ?", id);
        return Map.of("restored", true, "employeeId", restored.getId());
    }

    public List<Map<String, Object>> findArchivedEmployees() {
        return archiveJdbcTemplate.queryForList("""
                SELECT id AS "id",
                       name AS "name",
                       email AS "email",
                       department_name AS "departmentName",
                       archived_at AS "archivedAt"
                FROM archived_employees
                ORDER BY archived_at DESC
                """);
    }

    private Department findDepartment(ArchiveSnapshot snapshot) {
        if (snapshot.departmentId() != null) {
            var departmentById = departmentRepository.findById(snapshot.departmentId());
            if (departmentById.isPresent()) return departmentById.get();
        }
        return departmentRepository.findByNameIgnoreCase(snapshot.departmentName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Restore requires the department '" + snapshot.departmentName()
                                + "'. Recreate that department, then try again."));
    }

    private record ArchiveSnapshot(
            Long id,
            String name,
            String email,
            Long departmentId,
            String departmentName) {
    }
}
