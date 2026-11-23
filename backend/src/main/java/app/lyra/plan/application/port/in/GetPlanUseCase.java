package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;

import java.util.UUID;

public interface GetPlanUseCase {

    DietPlan getPlan(UUID id);
}
