package com.transport.reporting.common.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Utilitaires pour construire des predicates JPA Criteria (Specifications).
 * Chaque methode n'ajoute le filtre que si la valeur n'est pas null / vide.
 */
public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /** Ajoute un LIKE %valeur% insensible a la casse (recherche partielle texte). */
    public static void addLikeIgnoreCase(List<Predicate> predicates, CriteriaBuilder cb,
                                         Root<?> root, String field, String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase(Locale.ROOT) + "%"));
        }
    }

    /** Ajoute une egalite sur un champ enum. */
    public static <E extends Enum<E>> void addEnumEqual(List<Predicate> predicates, CriteriaBuilder cb,
                                                        Root<?> root, String field, E value) {
        if (value != null) {
            predicates.add(cb.equal(root.get(field), value));
        }
    }

    /** Ajoute une egalite stricte (id, boolean, etc.). */
    public static void addEqual(List<Predicate> predicates, CriteriaBuilder cb,
                                Root<?> root, String field, Object value) {
        if (value != null) {
            predicates.add(cb.equal(root.get(field), value));
        }
    }

    /**
     * Ajoute une plage de dates [from, to] (bornes inclusives).
     * Chaque borne est optionnelle.
     */
    public static void addInstantRange(List<Predicate> predicates, CriteriaBuilder cb,
                                       Root<?> root, String field, Instant from, Instant to) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(field), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(field), to));
        }
    }

    /** Combine tous les predicates avec AND (retourne true si la liste est vide). */
    public static <T> Predicate andAll(List<Predicate> predicates, CriteriaBuilder cb) {
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /** Egalite sur un attribut d'une association (join). */
    public static <T> void addJoinEqual(List<Predicate> predicates, CriteriaBuilder cb,
                                        Root<?> root, String joinField, String attribute, Object value) {
        if (value != null) {
            predicates.add(cb.equal(root.join(joinField).get(attribute), value));
        }
    }
}
