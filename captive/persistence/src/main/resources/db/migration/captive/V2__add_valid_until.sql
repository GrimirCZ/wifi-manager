alter table if exists captive.captive_authorization_token
    add column if not exists valid_until timestamptz not null default now();
