package com.tissue.workflow.application.dto;

public sealed interface NodeIdentifier {

    record ExistingId(Long id) implements NodeIdentifier {
        public ExistingId {
            if (id == null) {
                throw new IllegalArgumentException("ID cannot be null");
            }
        }
    }

    record TempKey(String key) implements NodeIdentifier {
        public TempKey {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key cannot be null or blank");
            }
        }
    }
}
