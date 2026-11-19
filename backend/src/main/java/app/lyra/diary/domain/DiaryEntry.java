package app.lyra.diary.domain;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.InvalidInputException;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiaryEntry {

    private static final int MAX_MEAL_NAME_LENGTH = 50;

    @EqualsAndHashCode.Include
    private final UUID id;
    private final LocalDate date;
    private final String mealName;
    private final UUID foodId;
    private final String foodName;
    private final Quantity foodReference;
    private final Nutrients nutrients;
    private final Quantity quantity;

    public DiaryEntry(UUID id, LocalDate date, String mealName, UUID foodId, String foodName,
                      Quantity foodReference, Nutrients nutrients, Quantity quantity) {
        if (id == null || date == null || foodId == null || foodName == null || foodName.isBlank()
                || foodReference == null || nutrients == null || quantity == null) {
            throw new InvalidInputException("id, date, food and quantity are required");
        }
        if (foodReference.amount().signum() == 0) {
            throw new InvalidInputException("Food reference amount must be positive");
        }
        if (quantity.amount().signum() == 0) {
            throw new InvalidInputException("Quantity must be positive");
        }
        if (quantity.unit() != foodReference.unit()) {
            throw new InvalidInputException("Quantity must be in " + foodReference.unit());
        }
        this.id = id;
        this.date = date;
        this.mealName = validMealName(mealName);
        this.foodId = foodId;
        this.foodName = foodName;
        this.foodReference = foodReference;
        this.nutrients = nutrients;
        this.quantity = quantity;
    }

    public static DiaryEntry log(Food food, LocalDate date, String mealName, Quantity quantity) {
        return new DiaryEntry(UUID.randomUUID(), date, mealName, food.getId(), food.getName(),
                food.getReferenceAmount(), food.getNutrients(), quantity);
    }

    public Macros macros() {
        return new Macros(
                scaled(nutrients.kcal()),
                scaled(nutrients.protein()),
                scaled(nutrients.carbohydrate()),
                scaled(nutrients.fat()));
    }

    static Macros totalOf(List<DiaryEntry> entries) {
        return entries.stream().map(DiaryEntry::macros).reduce(Macros.ZERO, Macros::plus);
    }

    private BigDecimal scaled(BigDecimal perReference) {
        return perReference.multiply(quantity.amount()).divide(foodReference.amount(), MathContext.DECIMAL128);
    }

    private static String validMealName(String mealName) {
        String trimmed = mealName == null ? "" : mealName.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_MEAL_NAME_LENGTH) {
            throw new InvalidInputException("Meal name must have between 1 and " + MAX_MEAL_NAME_LENGTH + " characters");
        }
        return trimmed;
    }
}
