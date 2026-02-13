create table if not exists admin.user_device
(
    user_id       uuid        not null,
    device_mac    text        not null,
    name          text        null,
    hostname      text        null,
    is_randomized boolean     not null,
    authorized_at timestamptz not null,
    last_seen_at  timestamptz not null,
    primary key (user_id, device_mac)
);

create unique index if not exists uq_admin_user_device_device_mac on admin.user_device (device_mac);
create index if not exists idx_admin_user_device_user_id on admin.user_device (user_id);
