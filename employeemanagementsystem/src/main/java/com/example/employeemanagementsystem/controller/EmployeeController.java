package com.example.employeemanagementsystem.controller;

import com.example.employeemanagementsystem.dto.EmployeeRequest;
import com.example.employeemanagementsystem.dto.EmployeeResponse;
import com.example.employeemanagementsystem.projection.EmployeeSummary;
import com.example.employeemanagementsystem.projection.EmployeeView;
import com.example.employeemanagementsystem.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.findAll();
    }

    @GetMapping("/by-department/{departmentId}")
    public List<EmployeeResponse> getEmployeesByDepartment(
            @PathVariable Long departmentId) {
        return employeeService.findByDepartmentId(departmentId);
    }

    @GetMapping("/by-department-name/{departmentName}")
    public List<EmployeeResponse> getEmployeesByDepartmentName(
            @PathVariable String departmentName) {
        return employeeService.findByDepartmentName(departmentName);
    }

    @GetMapping("/summary/by-department/{departmentId}")
    public List<EmployeeSummary> getEmployeeSummaries(
            @PathVariable Long departmentId) {
        return employeeService.findProjectionByDepartmentId(departmentId);
    }

    @GetMapping("/view/by-department/{departmentId}")
    public List<EmployeeView> getEmployeeViews(
            @PathVariable Long departmentId) {
        return employeeService.findEmployeeViewsByDepartmentId(departmentId);
    }

    @GetMapping("/search")
    public Page<EmployeeResponse> searchEmployees(
            @RequestParam(defaultValue = "") String name,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return employeeService.searchByName(name, pageable);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> createEmployeesBatch(
            @Valid @RequestBody List<@Valid EmployeeRequest> employees) {
        return Map.of("inserted", employeeService.saveBatch(employees));
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteById(id);
    }
}
