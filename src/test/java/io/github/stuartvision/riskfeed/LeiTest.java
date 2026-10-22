package io.github.stuartvision.riskfeed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.stuartvision.riskfeed.domain.Lei;

class LeiTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "5493001KJTIIGC8Y1R12",
            "549300A1B2C3D4E5F690",
            "213800QWERTYUIOP1289",
            "969500ZXCVBNMASDFG86"})
    void acceptsValidCheckDigits(String lei) {
        assertThat(Lei.isValid(lei)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5493001KJTIIGC8Y1R13",   // wrong check digits
            "5493001KJTIIGC8Y1R21",   // check digits swapped
            "5493001KJTIIGC8Y1R1",    // too short
            "5493001kjtiigc8y1r12",   // lower case
            "5493001KJTIIGC8Y1RAB",   // letters in the check digits
            ""})
    void rejectsInvalidCodes(String lei) {
        assertThat(Lei.isValid(lei)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsNull() {
        assertThat(Lei.isValid(null)).isFalse();
    }
}
