package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyTargetsTest {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal MINUS_ONE = new BigDecimal("-1");

    @Test
    void acceptsZeroAndPositiveTargets() {
        DailyTargets targets = new DailyTargets(new BigDecimal("2000"), BigDecimal.ZERO, ONE, new BigDecimal("70.5"));

        assertThat(targets.kcal()).isEqualByComparingTo("2000");
        assertThat(targets.protein()).isEqualByComparingTo("0");
        assertThat(targets.fat()).isEqualByComparingTo("70.5");
    }

    @Test
    void rejectsANegativeTargetNamingIt() {
        assertThatThrownBy(() -> new DailyTargets(ONE, ONE, MINUS_ONE, ONE))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The carbohydrate target must be zero or positive");
    }

    @Test
    void rejectsEveryMissingOrNegativeTarget() {
        List<ThrowingCallable> invalid = List.of(
                () -> new DailyTargets(null, ONE, ONE, ONE),
                () -> new DailyTargets(MINUS_ONE, ONE, ONE, ONE),
                () -> new DailyTargets(ONE, null, ONE, ONE),
                () -> new DailyTargets(ONE, MINUS_ONE, ONE, ONE),
                () -> new DailyTargets(ONE, ONE, null, ONE),
                () -> new DailyTargets(ONE, ONE, MINUS_ONE, ONE),
                () -> new DailyTargets(ONE, ONE, ONE, null),
                () -> new DailyTargets(ONE, ONE, ONE, MINUS_ONE));

        for (ThrowingCallable create : invalid) {
            assertThatThrownBy(create).isInstanceOf(InvalidInputException.class);
        }
    }
}
