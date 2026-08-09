package com.tissue.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

class EncryptedStringConverterTest {

    private static final String SALT = "5c0744940b5c369b";
    private static final String SECRET = "webhook-signing-secret";

    private final EncryptedStringConverter sut = new EncryptedStringConverter(Encryptors.delux("encryption-key", SALT));

    @Test
    @DisplayName("success: a value round-trips through encryption")
    void roundTripsValue() {
        // given
        String stored = sut.convertToDatabaseColumn(SECRET);

        // when
        String restored = sut.convertToEntityAttribute(stored);

        // then
        assertThat(restored).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("success: the stored form does not contain the plain text")
    void storedFormHidesPlainText() {
        // when
        String stored = sut.convertToDatabaseColumn(SECRET);

        // then
        assertThat(stored).startsWith(EncryptedStringConverter.PREFIX).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("success: a value written before encryption existed is read back as-is")
    void readsLegacyPlainTextUnchanged() {
        // when
        String restored = sut.convertToEntityAttribute(SECRET);

        // then
        assertThat(restored).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("success: encrypting the same value twice yields different stored forms")
    void usesFreshInitializationVector() {
        // when
        String first = sut.convertToDatabaseColumn(SECRET);
        String second = sut.convertToDatabaseColumn(SECRET);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("fail: a value encrypted under another key reports a clear reason")
    void failsClearlyWhenKeyChanged() {
        // given
        String storedUnderAnotherKey =
                new EncryptedStringConverter(Encryptors.delux("a-different-key", SALT)).convertToDatabaseColumn(SECRET);

        // when & then
        assertThatThrownBy(() -> sut.convertToEntityAttribute(storedUnderAnotherKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("regenerated");
    }
}
