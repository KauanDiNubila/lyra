package app.lyra.plan.application.service;

import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.application.port.out.PlanRepositoryContract;
import app.lyra.shared.domain.UserId;

import java.util.UUID;

class InMemoryPlanRepositoryTest implements PlanRepositoryContract {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    private final UserId owner = UserId.random();
    private final UserId otherOwner = UserId.random();

    @Override
    public PlanRepository repository() {
        return repository;
    }

    @Override
    public UUID savedFoodId() {
        return UUID.randomUUID();
    }

    @Override
    public UserId owner() {
        return owner;
    }

    @Override
    public UserId otherOwner() {
        return otherOwner;
    }
}
