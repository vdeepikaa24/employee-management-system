package com.example.employeemanagementsystem.dto;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        String createdBy,
        Instant createdDate,
        String lastModifiedBy,
        Instant lastModifiedDate) {
}
