package com.transport.reporting.common.enums;

import com.transport.reporting.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canaux de notification voyageur (liste fermée, IDs stables côté API).
 */
public enum PassengerNotificationChannel {
    PLATFORM(1),
    EMAIL(2),
    SMS(3),
    WHATSAPP(4);

    public static final String ERROR_CODE = "INVALID_NOTIFICATION_CHANNEL";
    public static final String INVALID_MESSAGE =
            "Canal de notification invalide : valeurs autorisées 1 (plateforme), 2 (email), 3 (SMS), 4 (WhatsApp).";

    private final int id;

    PassengerNotificationChannel(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PassengerNotificationChannel fromId(int id) {
        for (PassengerNotificationChannel channel : values()) {
            if (channel.id == id) {
                return channel;
            }
        }
        throw new BusinessException(INVALID_MESSAGE, ERROR_CODE);
    }

    /** Défaut métier : plateforme + e-mail. */
    public static List<Integer> defaultIds() {
        return List.of(PLATFORM.id, EMAIL.id);
    }

    public static List<Integer> normalizeIds(List<Integer> raw) {
        if (raw == null) {
            return defaultIds();
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer id : raw) {
            if (id == null) {
                throw new BusinessException(INVALID_MESSAGE, ERROR_CODE);
            }
            fromId(id);
            unique.add(id);
        }
        return List.copyOf(unique);
    }

    public static List<Integer> toIds(
            boolean platform, boolean email, boolean sms, boolean whatsapp) {
        List<Integer> ids = new ArrayList<>(4);
        if (platform) {
            ids.add(PLATFORM.id);
        }
        if (email) {
            ids.add(EMAIL.id);
        }
        if (sms) {
            ids.add(SMS.id);
        }
        if (whatsapp) {
            ids.add(WHATSAPP.id);
        }
        return List.copyOf(ids);
    }
}
