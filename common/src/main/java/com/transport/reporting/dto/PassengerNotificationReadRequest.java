package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Marquage lu. Si {@code markAll=true}, ignore {@code ids}.
 * Sinon marque uniquement les ids appartenant au voyageur courant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerNotificationReadRequest {

    private boolean markAll;
    private List<Long> ids;
}
