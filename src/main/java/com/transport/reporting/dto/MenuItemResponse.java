package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuItemResponse {

    private String code;
    private String label;
    private String url;
    private String icon;
    private String permission;
}
