package app.lyra.diary.application.service;

import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.application.port.out.DiaryEntryRepositoryContract;
import app.lyra.shared.domain.UserId;

import java.util.UUID;

class InMemoryDiaryEntryRepositoryTest implements DiaryEntryRepositoryContract {

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final UserId owner = UserId.random();
    private final UserId otherOwner = UserId.random();

    @Override
    public DiaryEntryRepository repository() {
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
