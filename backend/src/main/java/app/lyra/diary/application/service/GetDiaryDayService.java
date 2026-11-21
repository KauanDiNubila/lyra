package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryDay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class GetDiaryDayService implements GetDiaryDayUseCase {

    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public DiaryDay getDay(LocalDate date) {
        return new DiaryDay(date, diaryEntryRepository.findByDate(date));
    }
}
