package com.transport.reporting.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTypeScopeTest {

    @Test
    void adminIsUnrestricted() {
        ReportTypeScope scope = ReportTypeScope.unrestricted();
        assertTrue(scope.isUnrestricted());
        assertTrue(scope.allows(null));
        assertTrue(scope.allows(12L));
    }

    @Test
    void emptyScopeNeverMeansAllTypes() {
        ReportTypeScope scope = ReportTypeScope.none();
        assertFalse(scope.isUnrestricted());
        assertTrue(scope.isEmpty());
        assertFalse(scope.allows(1L));
        assertFalse(scope.allows(null));
    }

    @Test
    void restrictedScopeAllowsOnlyAssignedTypes() {
        ReportTypeScope scope = ReportTypeScope.of(Set.of(3L, 7L));
        assertTrue(scope.allows(3L));
        assertTrue(scope.allows(7L));
        assertFalse(scope.allows(9L));
        assertFalse(scope.allows(null));
    }
}
