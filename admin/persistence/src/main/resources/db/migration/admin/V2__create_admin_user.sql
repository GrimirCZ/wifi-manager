create table if not exists admin.admin_user
(
    id            uuid primary key,
    created_at    timestamptz not null,
    updated_at    timestamptz not null,
    last_login_at timestamptz not null
);

create table if not exists admin.admin_user_identity
(
    id            uuid primary key,
    user_id       uuid        not null references admin.admin_user (id) on delete cascade,
    issuer        text        not null,
    subject       text        not null,
    email         text        not null,
    username      text        not null,
    first_name    text        null,
    last_name     text        null,
    picture_url   text        null,
    roles         text[]      not null default '{}',
    created_at    timestamptz not null,
    last_login_at timestamptz not null,
    unique (issuer, subject)
);

create index if not exists idx_admin_user_identity_user_id on admin.admin_user_identity (user_id);
create index if not exists idx_admin_user_identity_issuer_subject on admin.admin_user_identity (issuer, subject);
