package app.lyra.identity.adapter.in.web;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Macros;
import app.lyra.diary.domain.Planned;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.Nutrients;
import app.lyra.identity.application.port.in.ExportAccountUseCase.AccountExport;
import app.lyra.identity.domain.User;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

record AccountExportResponse(
        int version,
        Instant exportedAt,
        ExportAccountJson account,
        List<ExportFoodJson> customFoods,
        List<ExportPlanJson> plans,
        List<ExportDiaryEntryJson> diaryEntries) {

    static final int VERSION = 1;

    static AccountExportResponse from(AccountExport export) {
        return new AccountExportResponse(
                VERSION,
                Instant.now(),
                ExportAccountJson.from(export.user()),
                export.customFoods().stream().map(ExportFoodJson::from).toList(),
                export.plans().stream().map(ExportPlanJson::from).toList(),
                export.diaryEntries().stream().map(ExportDiaryEntryJson::from).toList());
    }

    record ExportAccountJson(UUID id, String email, String name, Instant createdAt) {

        static ExportAccountJson from(User user) {
            return new ExportAccountJson(user.getId().value(), user.getEmail(), user.getName(), user.getCreatedAt());
        }
    }

    record ExportNutrientsJson(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                         BigDecimal fiber, BigDecimal sodium) {

        static ExportNutrientsJson from(Nutrients nutrients) {
            return new ExportNutrientsJson(nutrients.kcal(), nutrients.protein(), nutrients.carbohydrate(), nutrients.fat(),
                    nutrients.fiber(), nutrients.sodium());
        }
    }

    record ExportMacrosJson(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

        static ExportMacrosJson from(Macros macros) {
            return new ExportMacrosJson(macros.kcal(), macros.protein(), macros.carbohydrate(), macros.fat());
        }

        static ExportMacrosJson from(DailyTargets targets) {
            return new ExportMacrosJson(targets.kcal(), targets.protein(), targets.carbohydrate(), targets.fat());
        }
    }

    record ExportFoodJson(UUID id, String name, BigDecimal referenceAmount, String referenceUnit, ExportNutrientsJson nutrients) {

        static ExportFoodJson from(Food food) {
            return new ExportFoodJson(food.getId(), food.getName(), food.getReferenceAmount().amount(),
                    food.getReferenceAmount().unit().name(), ExportNutrientsJson.from(food.getNutrients()));
        }
    }

    record ExportPlanJson(UUID id, String name, boolean active, ExportMacrosJson targets, List<ExportMealJson> meals) {

        static ExportPlanJson from(DietPlan plan) {
            return new ExportPlanJson(plan.getId(), plan.getName(), plan.isActive(), ExportMacrosJson.from(plan.getTargets()),
                    plan.getMeals().stream().map(ExportMealJson::from).toList());
        }
    }

    record ExportMealJson(String name, LocalTime time, List<ExportItemJson> items) {

        static ExportMealJson from(PlannedMeal meal) {
            return new ExportMealJson(meal.name(), meal.time(), meal.items().stream().map(ExportItemJson::from).toList());
        }
    }

    record ExportItemJson(UUID foodId, BigDecimal quantityAmount, String quantityUnit) {

        static ExportItemJson from(PlannedItem item) {
            return new ExportItemJson(item.foodId(), item.quantity().amount(), item.quantity().unit().name());
        }
    }

    record ExportDiaryEntryJson(UUID id, LocalDate date, String mealName, UUID foodId, String foodName,
                          BigDecimal referenceAmount, String referenceUnit, ExportNutrientsJson nutrients,
                          BigDecimal quantityAmount, String quantityUnit, ExportPlannedJson planned) {

        static ExportDiaryEntryJson from(DiaryEntry entry) {
            return new ExportDiaryEntryJson(entry.getId(), entry.getDate(), entry.getMealName(), entry.getFoodId(),
                    entry.getFoodName(), entry.getFoodReference().amount(), entry.getFoodReference().unit().name(),
                    ExportNutrientsJson.from(entry.getNutrients()), entry.getQuantity().amount(),
                    entry.getQuantity().unit().name(), entry.getPlanned().map(ExportPlannedJson::from).orElse(null));
        }
    }

    record ExportPlannedJson(UUID foodId, String foodName, BigDecimal quantityAmount, String quantityUnit,
                       ExportMacrosJson macros, int slot) {

        static ExportPlannedJson from(Planned planned) {
            return new ExportPlannedJson(planned.foodId(), planned.foodName(), planned.quantity().amount(),
                    planned.quantity().unit().name(), ExportMacrosJson.from(planned.macros()), planned.slot());
        }
    }
}
