create schema if not exists auth;

create table if not exists auth.app_user
(
    user_id       uuid primary key,
    is_active     boolean     not null default true,
    created_at    timestamptz not null,
    last_login_at timestamptz not null
);

create table if not exists auth.user_identity
(
    id            uuid primary key,
    issuer        text        not null,
    subject       text        not null,
    user_id       uuid        not null references auth.app_user (user_id) on delete cascade,
    display_name  text        not null,
    email         text        not null,
    picture_url   text        null,
    roles         text[]      not null default '{}',
    created_at    timestamptz not null,
    updated_at    timestamptz not null,
    last_login_at timestamptz not null,
    unique (issuer, subject),
    unique (user_id)  -- Ensure one-to-one relationship between app_user and user_identity.
);

create index if not exists idx_user_identity_user_id on auth.user_identity (user_id);
create index if not exists idx_user_identity_issuer_subject on auth.user_identity (issuer, subject);
