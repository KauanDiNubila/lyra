package app.lyra.plan.application.port.out;

import app.lyra.plan.domain.DietPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    void save(DietPlan plan);

    Optional<DietPlan> findById(UUID id);

    List<DietPlan> findAll();

    Optional<DietPlan> findActive();
}
