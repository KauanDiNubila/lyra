package app.lyra.diary.domain;

import app.lyra.shared.domain.NotFoundException;

public class NoActivePlanException extends NotFoundException {

    public NoActivePlanException() {
        super("There is no active plan to fill the day");
    }
}
