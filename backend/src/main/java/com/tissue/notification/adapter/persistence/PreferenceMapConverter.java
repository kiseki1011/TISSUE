package com.tissue.notification.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Converter for storing preference configurations as a JSON string.
 *
 * <p>Used to map a nested Map structure {@code Map<String, Map<String, Boolean>>} to a database
 * TEXT column. Useful for storing user preferences, notification settings, or feature toggles where
 * the structure is hierarchical and values are boolean.
 *
 * <p><strong>Data Structure Example:</strong>
 *
 * <pre>
 * {
 *   "EMAIL": {
 *     "ISSUE_CREATED": true,
 *     "ISSUE_DELETED": false
 *   },
 *   "IN_APP": {
 *     "ISSUE_UPDATED": true
 *   }
 * }
 * </pre>
 *
 * <p><strong>Note:</strong> Currently, this uses a standard {@link AttributeConverter}
 * which stores data as a plain string. <br> I'm considering migrating to Hibernate 6's native JSON
 * mapping using {@code
 *
 * @JdbcTypeCode(SqlTypes.JSON)} for better performance and query capabilities.
 */
@Slf4j
@Converter
public class PreferenceMapConverter implements
    AttributeConverter<Map<String, Map<String, Boolean>>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Map<String, Boolean>> attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert Preference Map to JSON string", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Map<String, Boolean>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, Map<String, Boolean>>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON string to Preference Map", e);
            return new HashMap<>();
        }
    }
}
