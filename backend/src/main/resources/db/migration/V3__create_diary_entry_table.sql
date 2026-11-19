create table diary_entry (
    id               uuid         primary key,
    entry_order      bigint       generated always as identity,
    entry_date       date         not null,
    meal_name        varchar(50)  not null,
    food_id          uuid         not null references food (id),
    food_name        varchar(200) not null,
    reference_amount numeric      not null check (reference_amount > 0),
    reference_unit   varchar(20)  not null check (reference_unit in ('GRAM', 'MILLILITER', 'PORTION')),
    kcal             numeric      not null check (kcal >= 0),
    protein          numeric      not null check (protein >= 0),
    carbohydrate     numeric      not null check (carbohydrate >= 0),
    fat              numeric      not null check (fat >= 0),
    fiber            numeric      check (fiber >= 0),
    sodium           numeric      check (sodium >= 0),
    quantity_amount  numeric      not null check (quantity_amount > 0),
    quantity_unit    varchar(20)  not null check (quantity_unit in ('GRAM', 'MILLILITER', 'PORTION'))
);

create index diary_entry_entry_date_idx on diary_entry (entry_date);
