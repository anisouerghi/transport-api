package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Inbox cloche : compteur non lus + 20 dernières notifications. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerNotificationInboxResponse {

    private long unreadCount;
    private List<PassengerNotificationItemResponse> items;
}
