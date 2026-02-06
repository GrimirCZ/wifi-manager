create table if not exists admin.allowed_mac
(
    mac                text primary key,
    owner_user_id      uuid        not null,
    owner_email        text        not null,
    owner_display_name text        not null,
    note               text        not null default '',
    valid_until        timestamptz null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null
);

create index if not exists idx_allowed_mac_valid_until on admin.allowed_mac (valid_until);
