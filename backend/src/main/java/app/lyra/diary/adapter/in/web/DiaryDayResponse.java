package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.DiaryDay;

import java.time.LocalDate;
import java.util.List;

record DiaryDayResponse(LocalDate date, List<MealResponse> meals, MacrosJson total) {

    static DiaryDayResponse from(DiaryDay day) {
        List<MealResponse> meals = day.meals().stream().map(MealResponse::from).toList();
        return new DiaryDayResponse(day.date(), meals, MacrosJson.from(day.total()));
    }

    record MealResponse(String name, List<EntryResponse> entries, MacrosJson total) {

        static MealResponse from(DiaryDay.Meal meal) {
            List<EntryResponse> entries = meal.entries().stream().map(EntryResponse::from).toList();
            return new MealResponse(meal.name(), entries, MacrosJson.from(meal.total()));
        }
    }
}
