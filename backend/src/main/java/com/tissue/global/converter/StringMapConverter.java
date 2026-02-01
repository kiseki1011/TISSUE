package com.tissue.global.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Converter for storing generic string-based metadata as a JSON string.
 *
 * <p>This converter maps a {@code Map<String, String>} structure to a database TEXT column. It is
 * widely used for storing flexible, schema-less data such as:
 *
 * <ul>
 *   <li>Activity Log metadata (e.g., projectKey, issueKey, actorName)
 *   <li>Notification message data (e.g., template variables)
 *   <li>Any other auxiliary information that doesn't require a dedicated column.
 * </ul>
 *
 * <p><strong>Data Structure Example:</strong>
 *
 * <pre>
 * {
 *   "projectKey": "TISSUE",
 *   "issueKey": "TISSUE-123",
 *   "actorName": "Seungki Kim"
 * }
 * </pre>
 *
 * <p><strong>Note:</strong> Currently, this uses a standard {@link AttributeConverter}
 * which stores data as a plain string. <br>
 * I'm considering migrating to Hibernate 6's native JSON mapping using {@code
 *
 * @JdbcTypeCode(SqlTypes.JSON)} for better performance and query capabilities.
 */
@Slf4j
@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert Map to JSON string", e);
            return "{}";
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON string to Map", e);
            return new HashMap<>();
        }
    }
}
