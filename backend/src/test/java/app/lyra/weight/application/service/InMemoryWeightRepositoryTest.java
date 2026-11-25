package app.lyra.weight.application.service;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.application.port.out.WeightRepositoryContract;

class InMemoryWeightRepositoryTest implements WeightRepositoryContract {

    private final InMemoryWeightRepository repository = new InMemoryWeightRepository();
    private final UserId owner = UserId.random();
    private final UserId otherOwner = UserId.random();

    @Override
    public WeightRepository repository() {
        return repository;
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
