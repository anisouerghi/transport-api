package com.transport.reporting.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Convertit la liste d'identifiants de notifications en JSON pour la colonne
 * {@code passenger.notifications}.
 *
 * <p>Pourquoi un converter plutôt que {@code @JdbcTypeCode(SqlTypes.JSON)} ?
 * Hibernate fait alors renderer le paramètre en {@code cast(? as json)}, syntaxe
 * acceptée par MySQL mais <b>rejetée par MariaDB</b> :
 * {@code ERROR 1064 ... near 'json)'}. MariaDB implémente en plus {@code JSON}
 * comme un simple alias de {@code LONGTEXT} contraint par
 * {@code CHECK (json_valid(col))}. Un binding texte simple est donc portable
 * MySQL / MariaDB.
 */
@Converter
public class IntegerListJsonConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<Integer>> LIST_OF_INTEGER = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("notifications non sérialisables : " + attribute, ex);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return List.copyOf(MAPPER.readValue(dbData, LIST_OF_INTEGER));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("notifications illisibles en base : " + dbData, ex);
        }
    }
}