package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;

import java.util.UUID;

public interface ActivatePlanUseCase {

    DietPlan activate(UserId userId, UUID id);
}
