package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.InvalidInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DiaryController.class)
class DiaryExceptionHandler {

    @ExceptionHandler(InvalidInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail invalidInput(InvalidInputException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid input");
        return problem;
    }

    @ExceptionHandler({DiaryEntryNotFoundException.class, FoodNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Not found");
        return problem;
    }
}
