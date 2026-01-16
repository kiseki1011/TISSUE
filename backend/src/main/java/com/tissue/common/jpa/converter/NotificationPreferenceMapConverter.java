package com.tissue.common.jpa.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class NotificationPreferenceMapConverter
        implements AttributeConverter<Map<String, Map<String, Boolean>>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Map<String, Boolean>> attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert NotificationPreference Map to JSON string", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Map<String, Boolean>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, Map<String, Boolean>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON string to NotificationPreference Map", e);
            return new HashMap<>();
        }
    }
}
