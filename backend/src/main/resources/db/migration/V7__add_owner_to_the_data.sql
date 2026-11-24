delete from diary_entry;
delete from diet_plan;
delete from food where source = 'CUSTOM';

alter table food
    add column user_id uuid references app_user (id) on delete cascade;

alter table food
    add constraint food_owner_check check ((source = 'TACO') = (user_id is null));

create index food_user_id_idx on food (user_id);

alter table diet_plan
    add column user_id uuid not null references app_user (id) on delete cascade;

drop index diet_plan_single_active_idx;

create unique index diet_plan_single_active_per_user_idx on diet_plan (user_id) where active;

create index diet_plan_user_id_idx on diet_plan (user_id);

alter table diary_entry
    add column user_id uuid not null references app_user (id) on delete cascade;

drop index diary_entry_entry_date_planned_slot_idx;

drop index diary_entry_entry_date_idx;

create unique index diary_entry_user_id_entry_date_planned_slot_idx on diary_entry (user_id, entry_date, planned_slot);

create index diary_entry_user_id_entry_date_idx on diary_entry (user_id, entry_date);
