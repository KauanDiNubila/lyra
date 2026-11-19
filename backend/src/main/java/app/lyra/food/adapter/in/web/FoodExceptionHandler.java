package app.lyra.food.adapter.in.web;

import app.lyra.food.domain.InvalidFoodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FoodController.class)
class FoodExceptionHandler {

    @ExceptionHandler(InvalidFoodException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail invalidFood(InvalidFoodException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid food");
        return problem;
    }
}
