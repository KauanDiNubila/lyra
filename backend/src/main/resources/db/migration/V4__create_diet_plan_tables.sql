create table diet_plan (
    id                  uuid         primary key,
    plan_order          bigint       generated always as identity,
    name                varchar(100) not null,
    active              boolean      not null default false,
    target_kcal         numeric      not null check (target_kcal >= 0),
    target_protein      numeric      not null check (target_protein >= 0),
    target_carbohydrate numeric      not null check (target_carbohydrate >= 0),
    target_fat          numeric      not null check (target_fat >= 0)
);

create unique index diet_plan_single_active_idx on diet_plan (active) where active;

create table planned_meal (
    id         uuid        primary key,
    plan_id    uuid        not null references diet_plan (id) on delete cascade,
    meal_order integer     not null,
    name       varchar(50) not null,
    meal_time  time
);

create index planned_meal_plan_id_idx on planned_meal (plan_id);

create table planned_item (
    id              uuid        primary key,
    meal_id         uuid        not null references planned_meal (id) on delete cascade,
    item_order      integer     not null,
    food_id         uuid        not null references food (id),
    quantity_amount numeric     not null check (quantity_amount > 0),
    quantity_unit   varchar(20) not null check (quantity_unit in ('GRAM', 'MILLILITER', 'PORTION'))
);

create index planned_item_meal_id_idx on planned_item (meal_id);
