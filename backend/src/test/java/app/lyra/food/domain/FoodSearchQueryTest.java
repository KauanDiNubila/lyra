package app.lyra.food.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodSearchQueryTest {

    @Test
    void splitsIntoNormalizedTerms() {
        assertThat(FoodSearchQuery.of("  Feijão   CARIOCA ").terms()).containsExactly("feijao", "carioca");
    }

    @Test
    void punctuationSeparatesTermsAndNeverReachesTheQuery() {
        assertThat(FoodSearchQuery.of("arroz,cozido%_").terms()).containsExactly("arroz", "cozido");
    }

    @Test
    void acceptsAQueryOfExactlyTwoCharacters() {
        assertThat(FoodSearchQuery.of("ab").terms()).containsExactly("ab");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "a", " ,. ", "%"})
    void rejectsQueriesShorterThanTwoCharacters(String query) {
        assertThatThrownBy(() -> FoodSearchQuery.of(query)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void theConstructorRejectsNoTerms() {
        assertThatThrownBy(() -> new FoodSearchQuery(null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new FoodSearchQuery(List.of())).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void theConstructorRejectsABlankOrMissingTerm() {
        assertThatThrownBy(() -> new FoodSearchQuery(List.of("arroz", ""))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new FoodSearchQuery(List.of(" "))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new FoodSearchQuery(Arrays.asList("arroz", null))).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void theConstructorKeepsTermsAsGiven() {
        assertThat(new FoodSearchQuery(List.of("100%", "a\\b")).terms()).containsExactly("100%", "a\\b");
    }

    @Test
    void termsCannotBeChangedAfterCreation() {
        FoodSearchQuery query = FoodSearchQuery.of("arroz");

        assertThatThrownBy(() -> query.terms().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
