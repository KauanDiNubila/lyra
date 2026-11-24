package app.lyra.diary.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MacrosTest {

    @Test
    void plusAddsEachMacro() {
        Macros first = new Macros(new BigDecimal("100.5"), new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        Macros second = new Macros(new BigDecimal("50.25"), new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));

        Macros sum = first.plus(second);

        assertThat(sum.kcal()).isEqualByComparingTo("150.75");
        assertThat(sum.protein()).isEqualByComparingTo("11");
        assertThat(sum.carbohydrate()).isEqualByComparingTo("22");
        assertThat(sum.fat()).isEqualByComparingTo("33");
    }

    @Test
    void minusSubtractsEachMacroAndCanGoNegative() {
        Macros first = new Macros(new BigDecimal("100.5"), new BigDecimal("1"), new BigDecimal("20"), new BigDecimal("3"));
        Macros second = new Macros(new BigDecimal("50.25"), new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("3"));

        Macros difference = first.minus(second);

        assertThat(difference.kcal()).isEqualByComparingTo("50.25");
        assertThat(difference.protein()).isEqualByComparingTo("-9");
        assertThat(difference.carbohydrate()).isEqualByComparingTo("18");
        assertThat(difference.fat()).isEqualByComparingTo("0");
    }

    @Test
    void zeroChangesNothing() {
        Macros macros = new Macros(new BigDecimal("7"), new BigDecimal("8"), new BigDecimal("9"), new BigDecimal("10"));

        Macros sum = Macros.ZERO.plus(macros);

        assertThat(sum.kcal()).isEqualByComparingTo("7");
        assertThat(sum.fat()).isEqualByComparingTo("10");
    }
}
