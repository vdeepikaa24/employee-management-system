package com.example.employeemanagementsystem.controller;

import com.example.employeemanagementsystem.Department;
import com.example.employeemanagementsystem.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentService.findAll();
    }

    @GetMapping("/by-name/{name}")
    public Department getDepartmentByName(@PathVariable String name) {
        return departmentService.findByNameUsingNamedQuery(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + name));
    }

    @GetMapping("/with-employees/{name}")
    public Department getDepartmentWithEmployeesByName(@PathVariable String name) {
        return departmentService.findWithEmployeesByNameUsingNamedQuery(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + name));
    }

    @GetMapping("/{id}")
    public Department getDepartmentById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department createDepartment(@RequestBody Department department) {
        return departmentService.save(department);
    }

    @PutMapping("/{id}")
    public Department updateDepartment(
            @PathVariable Long id,
            @RequestBody Department updatedDepartment) {

        Department existingDepartment = departmentService.findById(id);
        existingDepartment.setName(updatedDepartment.getName());

        return departmentService.save(existingDepartment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.findById(id);
        departmentService.deleteById(id);
    }
}
