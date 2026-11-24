package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Origin;

import java.time.LocalDate;
import java.util.UUID;

record EntryResponse(
        UUID id,
        LocalDate date,
        String mealName,
        UUID foodId,
        String foodName,
        QuantityJson quantity,
        MacrosJson macros,
        Origin origin,
        PlannedResponse planned) {

    static EntryResponse from(DiaryEntry entry) {
        return new EntryResponse(entry.getId(), entry.getDate(), entry.getMealName(), entry.getFoodId(),
                entry.getFoodName(), QuantityJson.from(entry.getQuantity()), MacrosJson.from(entry.macros()),
                entry.origin(), entry.getPlanned().map(PlannedResponse::from).orElse(null));
    }
}
