create table food (
    id               uuid         primary key,
    name             varchar(200) not null,
    search_name      varchar(200) not null,
    source           varchar(20)  not null check (source in ('TACO', 'CUSTOM')),
    source_reference varchar(50)  unique,
    reference_amount numeric      not null check (reference_amount > 0),
    reference_unit   varchar(20)  not null check (reference_unit in ('GRAM', 'MILLILITER', 'PORTION')),
    kcal             numeric      not null check (kcal >= 0),
    protein          numeric      not null check (protein >= 0),
    carbohydrate     numeric      not null check (carbohydrate >= 0),
    fat              numeric      not null check (fat >= 0),
    fiber            numeric      check (fiber >= 0),
    sodium           numeric      check (sodium >= 0)
);
