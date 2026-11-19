package app.lyra.food.domain;

public class InvalidFoodException extends RuntimeException {

    public InvalidFoodException(String message) {
        super(message);
    }
}
