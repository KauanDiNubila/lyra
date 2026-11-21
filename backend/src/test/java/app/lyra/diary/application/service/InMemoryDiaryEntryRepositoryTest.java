package app.lyra.diary.application.service;

import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.application.port.out.DiaryEntryRepositoryContract;

import java.util.UUID;

class InMemoryDiaryEntryRepositoryTest implements DiaryEntryRepositoryContract {

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();

    @Override
    public DiaryEntryRepository repository() {
        return repository;
    }

    @Override
    public UUID savedFoodId() {
        return UUID.randomUUID();
    }
}
