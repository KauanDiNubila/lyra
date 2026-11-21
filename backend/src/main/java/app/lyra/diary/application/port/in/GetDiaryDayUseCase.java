package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryDay;

import java.time.LocalDate;

public interface GetDiaryDayUseCase {

    DiaryDay getDay(LocalDate date);
}
