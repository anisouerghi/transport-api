package com.transport.reporting.service;

import com.transport.reporting.entity.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicStatusMapperTest {

    private final PublicStatusMapper mapper = new PublicStatusMapper();

    @Test
    void mapsInternalCodesToPublicLabels() {
        assertEquals(PublicStatusMapper.RECEIVED, mapper.publicLabel("NEW"));
        assertEquals(PublicStatusMapper.IN_TREATMENT, mapper.publicLabel("AFFECTE"));
        assertEquals(PublicStatusMapper.IN_TREATMENT, mapper.publicLabel("IN_PROGRESS"));
        assertEquals(PublicStatusMapper.COMPLEMENT_ASKED, mapper.publicLabel("DEMANDE_COMPLEMENT"));
        assertEquals(PublicStatusMapper.COMPLEMENT_ASKED, mapper.publicLabel("DEMANDE_CLARIFICATION"));
        assertEquals(PublicStatusMapper.TREATED, mapper.publicLabel("RESOLVED"));
        assertEquals(PublicStatusMapper.CLOSED, mapper.publicLabel("CLOSED"));
        assertEquals(PublicStatusMapper.REJECTED, mapper.publicLabel("REJETE"));
    }

    @Test
    void prefersStoredStatutPublic() {
        Status status = Status.builder().code("IN_PROGRESS").statutPublic("Personnalisé").build();
        assertEquals("Personnalisé", mapper.publicLabel(status));
    }

    @Test
    void affecteToInProgressDoesNotChangePublicStatus() {
        assertFalse(mapper.publicStatusChanged(
                mapper.publicLabel("AFFECTE"),
                mapper.publicLabel("IN_PROGRESS")));
        assertTrue(mapper.publicStatusChanged(
                mapper.publicLabel("NEW"),
                mapper.publicLabel("AFFECTE")));
        assertTrue(mapper.publicStatusChanged(
                mapper.publicLabel("IN_PROGRESS"),
                mapper.publicLabel("DEMANDE_COMPLEMENT")));
    }
}
