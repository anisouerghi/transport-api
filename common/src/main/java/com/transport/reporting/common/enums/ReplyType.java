package com.transport.reporting.common.enums;

/**
 * Rôle d'un message dans la conversation d'un signalement.
 * <p>
 * Le type est décidé par le serveur. Il conditionne la visibilité voyageur
 * et le droit de répondre : une note interne ne sort jamais du suivi public,
 * et seul un voyageur authentifié peut enregistrer une réponse de complément.
 */
public enum ReplyType {
    /** Réponse agent visible du voyageur, sans demande de pièce ou d'explication. */
    RESPONSE,
    /** Demande agent au voyageur. Ouvre un cycle de complément. */
    COMPLEMENT_REQUEST,
    /** Réponse du voyageur à une demande de complément encore ouverte. */
    COMPLEMENT_RESPONSE,
    /** Note réservée aux agents. Jamais exposée par les API publiques. */
    INTERNAL_NOTE
}
