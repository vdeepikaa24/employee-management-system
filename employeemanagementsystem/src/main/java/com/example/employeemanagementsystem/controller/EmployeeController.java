package com.example.employeemanagementsystem.controller;

import com.example.employeemanagementsystem.Employee;
import com.example.employeemanagementsystem.projection.EmployeeSummary;
import com.example.employeemanagementsystem.projection.EmployeeView;
import com.example.employeemanagementsystem.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public List<Employee> getAllEmployees() {
        return employeeService.findAll();
    }

    @GetMapping("/by-department/{departmentId}")
    public List<Employee> getEmployeesByDepartment(
            @PathVariable Long departmentId) {
        return employeeService.findByDepartmentId(departmentId);
    }

    @GetMapping("/by-department-name/{departmentName}")
    public List<Employee> getEmployeesByDepartmentName(
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
    public Page<Employee> searchEmployees(
            @RequestParam(defaultValue = "") String name,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return employeeService.searchByName(name, pageable);
    }

    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.save(employee);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> createEmployeesBatch(
            @RequestBody List<Employee> employees) {
        return Map.of("inserted", employeeService.saveBatch(employees));
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee updatedEmployee) {

        Employee existingEmployee = employeeService.findById(id);
        existingEmployee.setName(updatedEmployee.getName());
        existingEmployee.setEmail(updatedEmployee.getEmail());

        return employeeService.save(existingEmployee);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.findById(id);
        employeeService.deleteById(id);
    }
}
