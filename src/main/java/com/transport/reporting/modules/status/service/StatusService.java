package com.transport.reporting.modules.status.service;

import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.status.dto.StatusResponse;
import com.transport.reporting.modules.status.entity.Status;
import com.transport.reporting.modules.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusService {

    private final StatusRepository statusRepository;

    public List<StatusResponse> findAll() {
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public Status findByCode(String code) {
        return statusRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Status", code));
    }

    public StatusResponse toResponse(Status status) {
        return StatusResponse.builder()
                .statusId(status.getStatusId())
                .code(status.getCode())
                .label(status.getLabel())
                .displayOrder(status.getDisplayOrder())
                .build();
    }
}
