package app.lyra.food.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarcodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "036000291452", "7891000100103", "10012345678902"})
    void acceptsBetweenEightAndFourteenDigits(String code) {
        assertThat(new Barcode(code).value()).isEqualTo(code);
    }

    @Test
    void ignoresTheSpacesAroundTheCode() {
        assertThat(new Barcode("  7891000100103 ")).isEqualTo(new Barcode("7891000100103"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "1234567", "123456789012345", "78910001001A3", "7891-0001-0010"})
    void refusesAnythingThatIsNotEightToFourteenDigits(String code) {
        assertThatThrownBy(() -> new Barcode(code))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Barcode must have between 8 and 14 digits");
    }

    @Test
    void printsJustTheCode() {
        assertThat(new Barcode("7891000100103")).hasToString("7891000100103");
    }
}
