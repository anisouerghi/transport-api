package com.transport.reporting.common.enums;

/**
 * Auteur réel d'un message, fixé par le serveur à partir du jeton.
 * Le corps de la requête ne peut pas choisir cet auteur.
 */
public enum ReplyAuthorType {
    /** Agent ou administrateur authentifié. */
    AGENT,
    /** Voyageur propriétaire du signalement. */
    PASSENGER,
    /** Message produit par le système, sans utilisateur ni voyageur. */
    SYSTEM
}
