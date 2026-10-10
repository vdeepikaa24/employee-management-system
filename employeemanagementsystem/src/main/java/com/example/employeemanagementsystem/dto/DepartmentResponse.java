package com.example.employeemanagementsystem.dto;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String createdBy,
        Instant createdDate,
        String lastModifiedBy,
        Instant lastModifiedDate) {
}
