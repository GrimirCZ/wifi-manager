create schema if not exists admin;

create table if not exists admin.shedlock (
    name text not null primary key,
    lock_until timestamptz not null,
    locked_at timestamptz not null,
    locked_by text not null
);

