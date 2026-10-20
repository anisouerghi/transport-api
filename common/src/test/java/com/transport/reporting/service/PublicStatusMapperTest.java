package com.transport.reporting.service;

import com.transport.reporting.entity.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicStatusMapperTest {

    private final PublicStatusMapper mapper = new PublicStatusMapper();

    @Test
    void readsOnlyStatutPublicFromEntity() {
        Status withColumn = Status.builder().code("IN_PROGRESS").statutPublic("En cours de traitement").build();
        assertEquals("En cours de traitement", mapper.publicLabel(withColumn));

        Status withoutColumn = Status.builder().code("IN_PROGRESS").statutPublic(null).build();
        assertNull(mapper.publicLabel(withoutColumn));
    }

    @Test
    void samePublicLabelMeansNoStatusNotification() {
        assertFalse(mapper.publicStatusChanged("En cours de traitement", "En cours de traitement"));
        assertTrue(mapper.publicStatusChanged("Reçu", "En cours de traitement"));
        assertTrue(mapper.publicStatusChanged(null, "Traité"));
        assertFalse(mapper.publicStatusChanged("Reçu", null));
    }
}
