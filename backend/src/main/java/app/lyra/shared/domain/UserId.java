package app.lyra.shared.domain;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new InvalidInputException("The user id is required");
        }
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidInputException("The user id is not valid");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
