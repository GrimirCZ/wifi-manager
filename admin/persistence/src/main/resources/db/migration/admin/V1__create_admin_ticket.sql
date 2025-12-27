create schema if not exists admin;

create table if not exists admin.ticket
(
    id           uuid primary key,
    access_code  text        not null unique,
    created_at   timestamptz not null,
    valid_until  timestamptz not null,
    was_canceled boolean     not null,
    author_id    uuid        not null
);

create index if not exists idx_ticket_author_id on admin.ticket (author_id);
create index if not exists idx_ticket_valid_until on admin.ticket (valid_until);

create table if not exists admin.authorized_device
(
    mac        text    not null,
    name       text    null,
    was_access_revoked boolean not null default false,
    ticket_id  uuid    not null references admin.ticket (id) on delete cascade,
    primary key (ticket_id, mac)
);
