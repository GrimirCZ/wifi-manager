create table if not exists captive.network_user
(
    user_id             uuid primary key,
    identity_id         uuid        not null,
    allowed_device_count integer     not null,
    admin_override_limit integer     null,
    created_at          timestamptz not null,
    updated_at          timestamptz not null,
    last_login_at       timestamptz not null
);

create table if not exists captive.network_user_device
(
    user_id       uuid        not null references captive.network_user (user_id) on delete cascade,
    device_mac    text        not null references captive.captive_device (mac) on delete cascade,
    name          text        null,
    hostname      text        null,
    is_randomized boolean     not null default false,
    authorized_at timestamptz not null,
    last_seen_at  timestamptz not null,
    primary key (user_id, device_mac)
);

create unique index if not exists uq_network_user_device_mac on captive.network_user_device (device_mac);
