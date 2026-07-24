package com.transport.reporting.service;

import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.ReportMapper;
import com.transport.reporting.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service metier Statut.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusService {

    private final StatusRepository statusRepository;
    private final ReportMapper reportMapper;

    public List<StatusResponse> findAll() {
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(reportMapper::toStatusResponse)
                .toList();
    }

    public Status findByCode(String code) {
        return statusRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Status", code));
    }
}
