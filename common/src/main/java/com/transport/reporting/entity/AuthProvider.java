package com.transport.reporting.entity;

/**
 * Fournisseur d'authentification du compte voyageur.
 */
public enum AuthProvider {
    /** Inscription / connexion e-mail + mot de passe. */
    LOCAL,
    /** Connexion Google OAuth 2.0 / OpenID Connect. */
    GOOGLE
}
