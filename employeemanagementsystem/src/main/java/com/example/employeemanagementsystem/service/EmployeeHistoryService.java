package com.example.employeemanagementsystem.service;

import com.example.employeemanagementsystem.EmployeeHistoryEvent;
import com.example.employeemanagementsystem.repository.EmployeeHistoryRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EmployeeHistoryService {

    private final EmployeeHistoryRepository historyRepository;

    public EmployeeHistoryService(EmployeeHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void record(Long employeeId, String employeeName, String action, String summary) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String changedBy = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName()
                : "system";

        EmployeeHistoryEvent event = new EmployeeHistoryEvent();
        event.setEmployeeId(employeeId);
        event.setEmployeeName(employeeName);
        event.setAction(action);
        event.setSummary(summary);
        event.setChangedBy(changedBy);
        event.setChangedAt(Instant.now());
        historyRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<EmployeeHistoryEvent> findByEmployeeId(Long employeeId) {
        return historyRepository.findByEmployeeIdOrderByChangedAtDescIdDesc(employeeId);
    }

    @Transactional
    public void copyHistory(Long previousEmployeeId, Long restoredEmployeeId) {
        List<EmployeeHistoryEvent> priorEvents = historyRepository
                .findByEmployeeIdOrderByChangedAtDescIdDesc(previousEmployeeId);
        List<EmployeeHistoryEvent> restoredEvents = priorEvents.stream().map(prior -> {
            EmployeeHistoryEvent copy = new EmployeeHistoryEvent();
            copy.setEmployeeId(restoredEmployeeId);
            copy.setEmployeeName(prior.getEmployeeName());
            copy.setAction(prior.getAction());
            copy.setSummary(prior.getSummary());
            copy.setChangedBy(prior.getChangedBy());
            copy.setChangedAt(prior.getChangedAt());
            return copy;
        }).toList();
        historyRepository.saveAll(restoredEvents);
    }
}
