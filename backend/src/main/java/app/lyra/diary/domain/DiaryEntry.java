package app.lyra.diary.domain;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    @Getter(AccessLevel.NONE)
    private final Planned planned;

    public DiaryEntry(UUID id, LocalDate date, String mealName, UUID foodId, String foodName,
                      Quantity foodReference, Nutrients nutrients, Quantity quantity) {
        this(id, date, mealName, foodId, foodName, foodReference, nutrients, quantity, null);
    }

    public DiaryEntry(UUID id, LocalDate date, String mealName, UUID foodId, String foodName,
                      Quantity foodReference, Nutrients nutrients, Quantity quantity, Planned planned) {
        if (id == null || date == null || foodId == null || foodName == null || foodName.isBlank()
                || foodReference == null || nutrients == null || quantity == null) {
            throw new InvalidInputException("id, date, food and quantity are required");
        }
        if (foodReference.amount().signum() == 0) {
            throw new InvalidInputException("Food reference amount must be positive");
        }
        if (quantity.amount().signum() == 0 && planned == null) {
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
        this.planned = planned;
    }

    public static DiaryEntry log(Food food, LocalDate date, String mealName, Quantity quantity) {
        return new DiaryEntry(UUID.randomUUID(), date, mealName, food.getId(), food.getName(),
                food.getReferenceAmount(), food.getNutrients(), quantity);
    }

    public static DiaryEntry fromPlan(Food food, LocalDate date, String mealName, Quantity quantity, int slot) {
        Macros macros = macrosOf(food.getNutrients(), food.getReferenceAmount(), quantity);
        return new DiaryEntry(UUID.randomUUID(), date, mealName, food.getId(), food.getName(),
                food.getReferenceAmount(), food.getNutrients(), quantity,
                new Planned(food.getId(), quantity, macros, slot));
    }

    public DiaryEntry withQuantity(Quantity newQuantity) {
        return new DiaryEntry(id, date, mealName, foodId, foodName, foodReference, nutrients, newQuantity, planned);
    }

    public DiaryEntry withFood(Food food, Quantity newQuantity) {
        return new DiaryEntry(id, date, mealName, food.getId(), food.getName(), food.getReferenceAmount(),
                food.getNutrients(), newQuantity, planned);
    }

    public DiaryEntry withMealName(String newMealName) {
        return new DiaryEntry(id, date, newMealName, foodId, foodName, foodReference, nutrients, quantity, planned);
    }

    public Optional<Planned> getPlanned() {
        return Optional.ofNullable(planned);
    }

    public Origin origin() {
        if (planned == null) {
            return Origin.EXTRA;
        }
        if (quantity.amount().signum() == 0) {
            return Origin.SKIPPED;
        }
        if (!foodId.equals(planned.foodId())) {
            return Origin.SWAPPED;
        }
        if (quantity.amount().compareTo(planned.quantity().amount()) != 0) {
            return Origin.ADJUSTED;
        }
        return Origin.PLANNED;
    }

    public Macros macros() {
        return macrosOf(nutrients, foodReference, quantity);
    }

    static Macros totalOf(List<DiaryEntry> entries) {
        return entries.stream().map(DiaryEntry::macros).reduce(Macros.ZERO, Macros::plus);
    }

    static Macros plannedTotalOf(List<DiaryEntry> entries) {
        return entries.stream()
                .flatMap(entry -> entry.getPlanned().stream())
                .map(Planned::macros)
                .reduce(Macros.ZERO, Macros::plus);
    }

    private static Macros macrosOf(Nutrients nutrients, Quantity reference, Quantity quantity) {
        return new Macros(
                scaled(nutrients.kcal(), reference, quantity),
                scaled(nutrients.protein(), reference, quantity),
                scaled(nutrients.carbohydrate(), reference, quantity),
                scaled(nutrients.fat(), reference, quantity));
    }

    private static BigDecimal scaled(BigDecimal perReference, Quantity reference, Quantity quantity) {
        return perReference.multiply(quantity.amount()).divide(reference.amount(), MathContext.DECIMAL128);
    }

    private static String validMealName(String mealName) {
        String trimmed = mealName == null ? "" : mealName.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_MEAL_NAME_LENGTH) {
            throw new InvalidInputException("Meal name must have between 1 and " + MAX_MEAL_NAME_LENGTH + " characters");
        }
        return trimmed;
    }
}
