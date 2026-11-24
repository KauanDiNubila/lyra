package app.lyra.plan.domain;

import app.lyra.shared.domain.NotFoundException;

import java.util.UUID;

public class PlanNotFoundException extends NotFoundException {

    public PlanNotFoundException(UUID id) {
        super("Plan not found: " + id);
    }
}
