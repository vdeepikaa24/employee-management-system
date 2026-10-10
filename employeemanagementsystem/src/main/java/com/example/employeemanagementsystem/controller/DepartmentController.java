package com.example.employeemanagementsystem.controller;

import com.example.employeemanagementsystem.dto.DepartmentRequest;
import com.example.employeemanagementsystem.dto.DepartmentResponse;
import com.example.employeemanagementsystem.service.DepartmentService;
import jakarta.validation.Valid;
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
    public List<DepartmentResponse> getAllDepartments() {
        return departmentService.findAll();
    }

    @GetMapping("/by-name/{name}")
    public DepartmentResponse getDepartmentByName(@PathVariable String name) {
        return departmentService.findByNameUsingNamedQuery(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + name));
    }

    @GetMapping("/with-employees/{name}")
    public DepartmentResponse getDepartmentWithEmployeesByName(@PathVariable String name) {
        return departmentService.findWithEmployeesByNameUsingNamedQuery(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found: " + name));
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(request);
    }

    @PutMapping("/{id}")
    public DepartmentResponse updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteById(id);
    }
}
