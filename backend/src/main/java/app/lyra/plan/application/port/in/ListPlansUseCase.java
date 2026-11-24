package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;

import java.util.List;

public interface ListPlansUseCase {

    List<DietPlan> listPlans();
}
