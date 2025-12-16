create schema if not exists captive;

create table if not exists captive.captive_authorization_token (
    id uuid primary key,
    access_code text not null unique,
    kicked_macs text[] not null default '{}'
);

create table if not exists captive.captive_device (
    mac text primary key,
    name text null
);

create table if not exists captive.captive_authorized_device (
    token_id uuid not null references captive.captive_authorization_token (id) on delete cascade,
    device_mac text not null references captive.captive_device (mac) on delete cascade,
    primary key (token_id, device_mac)
);

create index if not exists idx_captive_authorized_device_device_mac on captive.captive_authorized_device (device_mac);
