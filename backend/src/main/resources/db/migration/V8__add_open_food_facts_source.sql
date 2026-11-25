alter table food drop constraint food_owner_check;

alter table food drop constraint food_source_check;

alter table food
    add constraint food_source_check check (source in ('TACO', 'CUSTOM', 'OPEN_FOOD_FACTS'));

alter table food
    add constraint food_owner_check check ((source = 'CUSTOM') = (user_id is not null));
