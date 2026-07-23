package com.transport.reporting.modules.signalement.service;

import com.transport.reporting.modules.signalement.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SignalementService {

    SignalementResponse create(SignalementRequest request);

    SignalementResponse getByReference(String reference);

    SignalementResponse getById(Long id);

    Page<SignalementResponse> search(SignalementSearchDTO criteria, Pageable pageable);

    SignalementResponse changeStatut(Long id, ChangeStatutRequest request);

    SignalementResponse affecter(Long id, AffectationRequest request);

    SignalementResponse repondre(Long id, ReponseRequest request);
}
