package com.example.employeemanagementsystem.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String jobTitle,
        String phoneNumber,
        LocalDate hireDate,
        Long departmentId,
        String departmentName,
        String createdBy,
        Instant createdDate,
        String lastModifiedBy,
        Instant lastModifiedDate) {
}
