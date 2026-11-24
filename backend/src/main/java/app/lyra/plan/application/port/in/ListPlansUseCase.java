package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;

import java.util.List;

public interface ListPlansUseCase {

    List<DietPlan> listPlans(UserId userId);
}
