package com.example.employeemanagementsystem.repository;

import com.example.employeemanagementsystem.EmployeeHistoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistoryEvent, Long> {
    List<EmployeeHistoryEvent> findByEmployeeIdOrderByChangedAtDescIdDesc(Long employeeId);
}
