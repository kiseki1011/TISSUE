package com.tissue.support.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

class JsonNullablesTest {

    @Test
    @DisplayName("setOrKeep wraps a non-null value as a present patch")
    void setOrKeepPresentForValue() {
        JsonNullable<String> patch = JsonNullables.setOrKeep("hello");

        assertThat(patch.isPresent()).isTrue();
        assertThat(patch.get()).isEqualTo("hello");
    }

    @Test
    @DisplayName("setOrKeep leaves the field unchanged (undefined) for null")
    void setOrKeepUndefinedForNull() {
        JsonNullable<String> patch = JsonNullables.setOrKeep(null);

        assertThat(patch.isPresent()).isFalse();
    }
}
