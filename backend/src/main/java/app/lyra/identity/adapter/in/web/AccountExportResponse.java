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
        AccountJson account,
        List<FoodJson> customFoods,
        List<PlanJson> plans,
        List<DiaryEntryJson> diaryEntries) {

    static final int VERSION = 1;

    static AccountExportResponse from(AccountExport export) {
        return new AccountExportResponse(
                VERSION,
                Instant.now(),
                AccountJson.from(export.user()),
                export.customFoods().stream().map(FoodJson::from).toList(),
                export.plans().stream().map(PlanJson::from).toList(),
                export.diaryEntries().stream().map(DiaryEntryJson::from).toList());
    }

    record AccountJson(UUID id, String email, String name, Instant createdAt) {

        static AccountJson from(User user) {
            return new AccountJson(user.getId().value(), user.getEmail(), user.getName(), user.getCreatedAt());
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

    record FoodJson(UUID id, String name, BigDecimal referenceAmount, String referenceUnit, ExportNutrientsJson nutrients) {

        static FoodJson from(Food food) {
            return new FoodJson(food.getId(), food.getName(), food.getReferenceAmount().amount(),
                    food.getReferenceAmount().unit().name(), ExportNutrientsJson.from(food.getNutrients()));
        }
    }

    record PlanJson(UUID id, String name, boolean active, ExportMacrosJson targets, List<MealJson> meals) {

        static PlanJson from(DietPlan plan) {
            return new PlanJson(plan.getId(), plan.getName(), plan.isActive(), ExportMacrosJson.from(plan.getTargets()),
                    plan.getMeals().stream().map(MealJson::from).toList());
        }
    }

    record MealJson(String name, LocalTime time, List<ItemJson> items) {

        static MealJson from(PlannedMeal meal) {
            return new MealJson(meal.name(), meal.time(), meal.items().stream().map(ItemJson::from).toList());
        }
    }

    record ItemJson(UUID foodId, BigDecimal quantityAmount, String quantityUnit) {

        static ItemJson from(PlannedItem item) {
            return new ItemJson(item.foodId(), item.quantity().amount(), item.quantity().unit().name());
        }
    }

    record DiaryEntryJson(UUID id, LocalDate date, String mealName, UUID foodId, String foodName,
                          BigDecimal referenceAmount, String referenceUnit, ExportNutrientsJson nutrients,
                          BigDecimal quantityAmount, String quantityUnit, PlannedJson planned) {

        static DiaryEntryJson from(DiaryEntry entry) {
            return new DiaryEntryJson(entry.getId(), entry.getDate(), entry.getMealName(), entry.getFoodId(),
                    entry.getFoodName(), entry.getFoodReference().amount(), entry.getFoodReference().unit().name(),
                    ExportNutrientsJson.from(entry.getNutrients()), entry.getQuantity().amount(),
                    entry.getQuantity().unit().name(), entry.getPlanned().map(PlannedJson::from).orElse(null));
        }
    }

    record PlannedJson(UUID foodId, String foodName, BigDecimal quantityAmount, String quantityUnit,
                       ExportMacrosJson macros, int slot) {

        static PlannedJson from(Planned planned) {
            return new PlannedJson(planned.foodId(), planned.foodName(), planned.quantity().amount(),
                    planned.quantity().unit().name(), ExportMacrosJson.from(planned.macros()), planned.slot());
        }
    }
}
