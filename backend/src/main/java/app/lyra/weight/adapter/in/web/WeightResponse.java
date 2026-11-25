package app.lyra.weight.adapter.in.web;

import app.lyra.weight.domain.WeightEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

record WeightResponse(LocalDate date, BigDecimal kilograms) {

    static WeightResponse from(WeightEntry entry) {
        return new WeightResponse(entry.date(), entry.kilograms());
    }
}
