create table weight_entry (
    user_id    uuid          not null references app_user (id) on delete cascade,
    entry_date date          not null,
    kilograms  numeric(5, 2) not null check (kilograms between 20 and 500),
    primary key (user_id, entry_date)
);
