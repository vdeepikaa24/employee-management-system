package com.example.employeemanagementsystem.controller;

import com.example.employeemanagementsystem.service.ArchiveService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/archive/employees")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> archiveEmployee(@PathVariable Long id) {
        return archiveService.archiveEmployee(id);
    }

    @PostMapping("/{id}/restore")
    public Map<String, Object> restoreEmployee(@PathVariable Long id) {
        return archiveService.restoreEmployee(id);
    }

    @GetMapping
    public List<Map<String, Object>> getArchivedEmployees() {
        return archiveService.findArchivedEmployees();
    }
}
