package com.example.employeemanagementsystem.repository;

import com.example.employeemanagementsystem.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    Optional<Department> findByNameIgnoreCase(String name);

    @Query(name = "Department.findByName")
    Optional<Department> findUsingNamedQuery(@Param("name") String name);

    @Query(name = "Department.findWithEmployeesByName")
    Optional<Department> findWithEmployeesUsingNamedQuery(
            @Param("name") String name);
}
