package app.lyra.diary.domain;

import app.lyra.shared.domain.InvalidInputException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DiaryDay(LocalDate date, List<DiaryEntry> entries) {

    public DiaryDay {
        if (date == null || entries == null) {
            throw new InvalidInputException("date and entries are required");
        }
        if (entries.stream().anyMatch(entry -> !entry.getDate().equals(date))) {
            throw new InvalidInputException("Every entry must be from " + date);
        }
        entries = List.copyOf(entries);
    }

    public List<Meal> meals() {
        Map<String, List<DiaryEntry>> byName = new LinkedHashMap<>();
        for (DiaryEntry entry : entries) {
            byName.computeIfAbsent(entry.getMealName(), name -> new ArrayList<>()).add(entry);
        }
        return byName.entrySet().stream().map(meal -> new Meal(meal.getKey(), meal.getValue())).toList();
    }

    public Macros total() {
        return DiaryEntry.totalOf(entries);
    }

    public record Meal(String name, List<DiaryEntry> entries) {

        public Meal {
            entries = List.copyOf(entries);
        }

        public Macros total() {
            return DiaryEntry.totalOf(entries);
        }
    }
}
