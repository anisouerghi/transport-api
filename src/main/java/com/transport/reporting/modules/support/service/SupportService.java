package com.transport.reporting.modules.support.service;

import com.transport.reporting.modules.support.dto.SupportRequest;
import com.transport.reporting.modules.support.dto.SupportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SupportService {

    SupportResponse create(SupportRequest request);

    SupportResponse update(Long id, SupportRequest request);

    SupportResponse getById(Long id);

    SupportResponse getByUuid(UUID uuid);

    SupportResponse getActiveByUuid(UUID uuid);

    Page<SupportResponse> findAll(Pageable pageable);

    SupportResponse activate(Long id);

    SupportResponse deactivate(Long id);
}
