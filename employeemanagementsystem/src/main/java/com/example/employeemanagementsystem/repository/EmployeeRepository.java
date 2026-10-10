package com.example.employeemanagementsystem.repository;

import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.projection.EmployeeSummary;
import com.example.employeemanagementsystem.projection.EmployeeView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    @EntityGraph(attributePaths = "department")
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findWithDepartmentById(@Param("id") Long id);

    List<Employee> findByDepartmentNameIgnoreCase(String name);

    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId")
    List<Employee> findEmployeesByDepartmentId(
            @Param("departmentId") Long departmentId);

    @Query(name = "Employee.findByDepartmentName")
    List<Employee> findUsingNamedQuery(
            @Param("departmentName") String departmentName);

    List<EmployeeSummary> findProjectedByDepartmentId(Long departmentId);

    @Query("""
           SELECT new com.example.employeemanagementsystem.projection.EmployeeView(
               e.id, e.name, e.email, e.department.name
           )
           FROM Employee e
           WHERE e.department.id = :departmentId
           """)
    List<EmployeeView> findEmployeeViewsByDepartmentId(
            @Param("departmentId") Long departmentId);

    Page<Employee> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable);
}
