package com.tissue.workspace.domain.converter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.workspace.domain.ProjectJoinConfig;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProjectJoinConfigListConverter implements AttributeConverter<List<ProjectJoinConfig>, String> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<ProjectJoinConfig> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to convert project configs to JSON", e);
		}
	}

	@Override
	public List<ProjectJoinConfig> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return objectMapper.readValue(dbData, new TypeReference<List<ProjectJoinConfig>>() {
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to convert JSON to project configs", e);
		}
	}
}
