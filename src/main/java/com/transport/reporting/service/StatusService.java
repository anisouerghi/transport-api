package com.transport.reporting.service;

import com.transport.reporting.dto.StatusRequest;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Status;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.StatusMapper;
import com.transport.reporting.repository.StatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatusService {

    private final StatusRepository statusRepository;
    private final StatusMapper statusMapper;
    public StatusService(StatusRepository statusRepository, StatusMapper statusMapper) {
        this.statusRepository = statusRepository;
        this.statusMapper = statusMapper;
    }


    public List<StatusResponse> findAll() {
        return statusRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(statusMapper::toResponse)
                .collect(Collectors.toList());
    }

    public StatusResponse findById(Long id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status", id));

        return statusMapper.toResponse(status);
    }

    @Transactional
    public StatusResponse create(StatusRequest request) {

        Status status = statusMapper.toEntity(request);

        status = statusRepository.save(status);

        return statusMapper.toResponse(status);
    }

    @Transactional
    public StatusResponse update(Long id, StatusRequest request) {

        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status", id));

        statusMapper.updateEntity(status, request);

        status = statusRepository.save(status);

        return statusMapper.toResponse(status);
    }

    @Transactional
    public void delete(Long id) {

        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status", id));

        statusRepository.delete(status);
    }

    public Status findByCode(String code) {
        return statusRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Status", code));
    }
}