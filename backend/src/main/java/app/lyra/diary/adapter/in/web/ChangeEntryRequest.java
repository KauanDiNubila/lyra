package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.ChangeEntryUseCase.ChangeEntryCommand;

import java.util.UUID;

record ChangeEntryRequest(UUID foodId, QuantityJson quantity, String mealName) {

    ChangeEntryCommand toCommand() {
        return new ChangeEntryCommand(foodId, quantity == null ? null : quantity.toDomain(), mealName);
    }
}
