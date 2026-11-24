package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.DiaryDay;

import java.time.LocalDate;
import java.util.List;

record DiaryDayResponse(
        LocalDate date,
        List<MealResponse> meals,
        MacrosJson total,
        MacrosJson plannedTotal,
        MacrosJson differenceFromPlan,
        MacrosJson targets,
        MacrosJson differenceFromTargets) {

    static DiaryDayResponse from(DiaryDay day) {
        List<MealResponse> meals = day.meals().stream().map(MealResponse::from).toList();
        return new DiaryDayResponse(day.date(), meals, MacrosJson.from(day.total()),
                MacrosJson.from(day.plannedTotal()), MacrosJson.from(day.differenceFromPlan()),
                day.targets().map(MacrosJson::from).orElse(null),
                day.differenceFromTargets().map(MacrosJson::from).orElse(null));
    }

    record MealResponse(
            String name,
            List<EntryResponse> entries,
            MacrosJson total,
            MacrosJson plannedTotal,
            MacrosJson differenceFromPlan) {

        static MealResponse from(DiaryDay.Meal meal) {
            List<EntryResponse> entries = meal.entries().stream().map(EntryResponse::from).toList();
            return new MealResponse(meal.name(), entries, MacrosJson.from(meal.total()),
                    MacrosJson.from(meal.plannedTotal()), MacrosJson.from(meal.differenceFromPlan()));
        }
    }
}
