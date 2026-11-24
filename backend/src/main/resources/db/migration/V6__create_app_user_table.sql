create table app_user (
    id               uuid         primary key,
    provider         varchar(20)  not null check (provider in ('GOOGLE')),
    provider_subject varchar(255) not null,
    email            varchar(320) not null,
    name             varchar(200) not null,
    created_at       timestamptz  not null,
    constraint app_user_provider_subject_key unique (provider, provider_subject)
);
