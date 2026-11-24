package app.lyra.plan.application.port.out;

import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    void lockPlanChanges(UserId user);

    void save(UserId owner, DietPlan plan);

    Optional<DietPlan> findById(UserId user, UUID id);

    List<DietPlan> findAll(UserId user);

    Optional<DietPlan> findActive(UserId user);
}
