package com.example.employeemanagementsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotNull(message = "Department id is required")
        @Positive(message = "Department id must be positive")
        Long departmentId,

        @Size(max = 120, message = "Job title must be at most 120 characters")
        String jobTitle,

        @Size(max = 30, message = "Phone number must be at most 30 characters")
        @Pattern(regexp = "^$|^[+0-9().\\-\\s]{7,30}$", message = "Phone number contains unsupported characters")
        String phoneNumber,

        LocalDate hireDate) {
}
