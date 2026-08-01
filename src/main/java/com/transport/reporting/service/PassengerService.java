package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.PassengerCriteria;
import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.dto.PassengerResponse;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.PassengerMapper;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.specification.PassengerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Service métier Voyageur (création publique + administration).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PassengerService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "passengerId",
            "passengerId", "passengerId",
            "name", "name",
            "email", "email",
            "phoneNumber", "phoneNumber",
            "active", "active"
    );

    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final AuditLogService auditLogService;

    public Passenger findOrCreate(PassengerRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return passengerRepository.findByEmailIgnoreCase(request.getEmail())
                    .map(existing -> {
                        if (request.getName() != null) {
                            existing.setName(request.getName());
                        }
                        if (request.getPhoneNumber() != null) {
                            existing.setPhoneNumber(request.getPhoneNumber());
                        }
                        return passengerRepository.save(existing);
                    })
                    .orElseGet(() -> passengerRepository.save(passengerMapper.toEntity(request)));
        }
        return passengerRepository.save(passengerMapper.toEntity(request));
    }

    @Transactional(readOnly = true)
    public PageResponse<PassengerResponse> search(SearchRequest<PassengerCriteria> request) {
        PassengerCriteria criteria = request != null ? request.getFilters() : null;
        Pageable pageable = PageableUtils.toPageable(
                request != null ? request.getPageable() : null,
                "passengerId",
                SORT_FIELDS
        );
        Specification<Passenger> spec = PassengerSpecification.fromCriteria(criteria);
        Page<PassengerResponse> page = passengerRepository.findAll(spec, pageable)
                .map(passengerMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PassengerResponse findById(Long id) {
        return passengerMapper.toResponse(getEntity(id));
    }

    public PassengerResponse setActive(Long id, boolean active) {
        Passenger passenger = getEntity(id);
        boolean previous = passenger.isActive();
        passenger.setActive(active);
        passenger = passengerRepository.save(passenger);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.PASSENGERS)
                .entityName("Passenger")
                .entityId(String.valueOf(id))
                .oldValue("active=" + previous)
                .newValue("active=" + active)
                .description((active ? "Activation" : "Désactivation") + " du voyageur " + id)
                .build());
        return passengerMapper.toResponse(passenger);
    }

    private Passenger getEntity(Long id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));
    }
}
