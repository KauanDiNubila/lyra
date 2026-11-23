package app.lyra.food.domain;

import app.lyra.shared.domain.InvalidInputException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Food {

    private static final int MAX_NAME_LENGTH = 200;

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String name;
    private final FoodSource source;
    private final Quantity referenceAmount;
    private final Nutrients nutrients;

    public Food(UUID id, String name, FoodSource source, Quantity referenceAmount, Nutrients nutrients) {
        if (id == null || source == null || referenceAmount == null || nutrients == null) {
            throw new InvalidInputException("id, source, reference amount and nutrients are required");
        }
        if (referenceAmount.amount().signum() == 0) {
            throw new InvalidInputException("Reference amount must be positive");
        }
        this.id = id;
        this.name = validName(name);
        this.source = source;
        this.referenceAmount = referenceAmount;
        this.nutrients = nutrients;
    }

    public static Food custom(String name, Quantity referenceAmount, Nutrients nutrients) {
        return new Food(UUID.randomUUID(), name, FoodSource.CUSTOM, referenceAmount, nutrients);
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2 || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException("Food name must have between 2 and " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }
}
