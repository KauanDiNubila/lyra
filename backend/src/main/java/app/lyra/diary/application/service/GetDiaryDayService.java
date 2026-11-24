package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.Macros;
import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.domain.DailyTargets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class GetDiaryDayService implements GetDiaryDayUseCase {

    private final GetActivePlanUseCase getActivePlan;
    private final DiaryEntryRepository diaryEntryRepository;

    @Override
    public DiaryDay getDay(LocalDate date) {
        Optional<Macros> targets = getActivePlan.getActivePlan().map(plan -> toMacros(plan.getTargets()));
        return new DiaryDay(date, diaryEntryRepository.findByDate(date), targets);
    }

    private static Macros toMacros(DailyTargets targets) {
        return new Macros(targets.kcal(), targets.protein(), targets.carbohydrate(), targets.fat());
    }
}
