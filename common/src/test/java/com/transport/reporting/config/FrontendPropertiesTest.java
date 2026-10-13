package com.transport.reporting.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrontendPropertiesTest {

    @Test
    void trackingUrlUsesTheConfiguredPublicBaseAndTheFollowUpRoute() {
        FrontendProperties properties = new FrontendProperties();
        properties.setPublicBaseUrl("http://localhost:4200/");
        assertEquals(
                "http://localhost:4200/report-followup/f169c2df-e5e6-4aa0-b7ca-1b12b5f6fd5a",
                properties.buildTrackingUrl("f169c2df-e5e6-4aa0-b7ca-1b12b5f6fd5a"));

        properties.setPublicBaseUrl("https://signalement.transport.tn");
        assertEquals(
                "https://signalement.transport.tn/report-followup/abc",
                properties.buildTrackingUrl("abc"));
    }
}
