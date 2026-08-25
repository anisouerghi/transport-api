package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Matrice RBAC modules × actions pour l'administration des rôles.
 */
@Data
@Builder
public class PermissionMatrixResponse {

    /** Colonnes d'actions ordonnées (VIEW, ADD, EDIT…). */
    private List<String> actions;

    private List<ModuleRow> modules;

    @Data
    @Builder
    public static class ModuleRow {
        private String moduleCode;
        private String moduleLabel;
        /** actionCode → permission (null si non définie pour ce module). */
        private Map<String, PermissionCell> permissions;
    }

    @Data
    @Builder
    public static class PermissionCell {
        private Long permissionId;
        private String code;
        private String label;
        private boolean active;
    }
}
