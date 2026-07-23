package com.transport.reporting.modules.voyageur.service;

import com.transport.reporting.modules.voyageur.dto.VoyageurRequest;
import com.transport.reporting.modules.voyageur.dto.VoyageurResponse;
import com.transport.reporting.modules.voyageur.entity.Voyageur;

public interface VoyageurService {

    Voyageur findOrCreate(VoyageurRequest request);

    VoyageurResponse getById(Long id);
}
