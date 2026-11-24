package app.lyra.plan.application.service;

import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.application.port.out.PlanRepositoryContract;

import java.util.UUID;

class InMemoryPlanRepositoryTest implements PlanRepositoryContract {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    @Override
    public PlanRepository repository() {
        return repository;
    }

    @Override
    public UUID savedFoodId() {
        return UUID.randomUUID();
    }
}
