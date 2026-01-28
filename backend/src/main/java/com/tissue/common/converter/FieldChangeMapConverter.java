package com.tissue.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.common.dto.FieldChange;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Converter for storing field change history as a JSON string.
 *
 * <p>This converter maps a {@code Map<String, FieldChange>} structure to a database TEXT column. It
 * is primarily used in {@code ActivityLog} to persist details about what fields were modified during
 * an event (e.g., issue updates).
 *
 * <p><strong>Data Structure Example:</strong>
 *
 * <pre>
 * {
 *   "title": {
 *     "from": "Old Title",
 *     "to": "New Title"
 *   },
 *   "priority": {
 *     "from": "NORMAL",
 *     "to": "HIGH"
 *   }
 * }
 * </pre>
 *
 * <p><strong>Note:</strong> Currently, this uses a standard {@link AttributeConverter}
 * which stores data as a plain string. <br>
 * I'm considering migrating to Hibernate 6's native JSON mapping using {@code
 * @JdbcTypeCode(SqlTypes.JSON)} for better performance and query capabilities.
 */
@Slf4j
@Converter
public class FieldChangeMapConverter implements AttributeConverter<Map<String, FieldChange>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, FieldChange> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert Map to JSON string", e);
            throw new IllegalArgumentException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, FieldChange> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, FieldChange>>() {});
        } catch (IOException e) {
            log.error("Failed to convert JSON string to Map", e);
            return Collections.emptyMap();
        }
    }
}
