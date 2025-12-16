create schema if not exists admin;

create table if not exists admin.admin_ticket (
    id uuid primary key,
    access_code text not null unique,
    created_at timestamptz not null,
    valid_until timestamptz not null,
    was_canceled boolean not null,
    author_id uuid not null
);

create index if not exists idx_admin_ticket_author_id on admin.admin_ticket (author_id);
create index if not exists idx_admin_ticket_valid_until on admin.admin_ticket (valid_until);
