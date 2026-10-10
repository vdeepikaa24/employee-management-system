package com.example.employeemanagementsystem.projection;

public record EmployeeView(
        Long id,
        String name,
        String email,
        String departmentName
) {
}