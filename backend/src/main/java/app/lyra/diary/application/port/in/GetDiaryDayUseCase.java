package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryDay;
import app.lyra.shared.domain.UserId;

import java.time.LocalDate;

public interface GetDiaryDayUseCase {

    DiaryDay getDay(UserId userId, LocalDate date);
}
