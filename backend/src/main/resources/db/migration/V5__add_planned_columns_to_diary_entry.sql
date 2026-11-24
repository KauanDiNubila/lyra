alter table diary_entry
    add column planned_food_id         uuid references food (id),
    add column planned_food_name       varchar(200),
    add column planned_quantity_amount numeric check (planned_quantity_amount > 0),
    add column planned_quantity_unit   varchar(20) check (planned_quantity_unit in ('GRAM', 'MILLILITER', 'PORTION')),
    add column planned_kcal            numeric check (planned_kcal >= 0),
    add column planned_protein         numeric check (planned_protein >= 0),
    add column planned_carbohydrate    numeric check (planned_carbohydrate >= 0),
    add column planned_fat             numeric check (planned_fat >= 0),
    add column planned_slot            integer check (planned_slot >= 0);

alter table diary_entry
    add constraint diary_entry_planned_all_or_none_check check (
        (planned_slot is null and planned_food_id is null and planned_food_name is null
            and planned_quantity_amount is null and planned_quantity_unit is null and planned_kcal is null
            and planned_protein is null and planned_carbohydrate is null and planned_fat is null)
        or
        (planned_slot is not null and planned_food_id is not null and planned_food_name is not null
            and planned_quantity_amount is not null and planned_quantity_unit is not null
            and planned_kcal is not null and planned_protein is not null and planned_carbohydrate is not null
            and planned_fat is not null));

alter table diary_entry drop constraint diary_entry_quantity_amount_check;

alter table diary_entry
    add constraint diary_entry_quantity_amount_check check (
        quantity_amount > 0 or (quantity_amount = 0 and planned_slot is not null));

create unique index diary_entry_entry_date_planned_slot_idx on diary_entry (entry_date, planned_slot);
