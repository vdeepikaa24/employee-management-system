package com.example.employeemanagementsystem.projection;

import org.springframework.beans.factory.annotation.Value;

public interface EmployeeSummary {

    Long getId();

    String getName();

    String getEmail();

    @Value("#{target.department.name}")
    String getDepartmentName();
}