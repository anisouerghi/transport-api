package com.transport.reporting.common.enums;

import com.transport.reporting.exception.BusinessException;

import java.util.Locale;
import java.util.Set;

/**
 * Langues voyageur acceptées (ISO 639-1, 2 lettres) : FR, AR, EN.
 * Utilisé par {@code @Pattern(regexp = PassengerLanguage.REGEX)} et par la
 * normalisation métier avant écriture en base.
 */
public final class PassengerLanguage {

    public static final String FR = "FR";
    public static final String AR = "AR";
    public static final String EN = "EN";

    /** Liste fermée, insensible à la casse ({@code fr} accepté comme {@code FR}). */
    public static final Set<String> SUPPORTED = Set.of(FR, AR, EN);

    /** Motif Bean Validation : 2 lettres issues de la liste fermée. */
    public static final String REGEX = "(?i)^(FR|AR|EN)$";

    public static final String INVALID_MESSAGE = "Langue invalide : valeurs autorisées FR, AR, EN.";

    /** Code d'erreur applicatif renvoyé quand la langue est hors liste fermée. */
    public static final String ERROR_CODE = "INVALID_LANGUAGE";

    /** Taille maximale du tableau {@code notifications} (garde-fou payload). */
    public static final int MAX_NOTIFICATIONS = 100;

    private PassengerLanguage() {
    }

    /**
     * Normalise une saisie libre en code canonique en majuscules.
     *
     * @return {@code null} si la saisie est absente ou vide (champ non renseigné)
     * @throws IllegalArgumentException si la valeur n'est pas dans {@link #SUPPORTED}
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!SUPPORTED.contains(upper)) {
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }
        return upper;
    }

    /**
     * Variante {@link #normalize(String)} traduisant le refus en {@link BusinessException}
     * (code {@code INVALID_LANGUAGE}) pour les chemins de service / mapper.
     *
     * @return {@code null} si la saisie est absente ou vide
     */
    public static String normalizeOrThrow(String raw) {
        try {
            return normalize(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage(), ERROR_CODE);
        }
    }
}
