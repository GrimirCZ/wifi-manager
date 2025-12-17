create table if not exists admin.admin_user (
    id uuid primary key,
    email text not null,
    display_name text not null,
    picture_url text null,
    is_active boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_login_at timestamptz not null
);

create unique index if not exists ux_admin_user_email on admin.admin_user (email);

create table if not exists admin.admin_user_identity (
    id uuid primary key,
    user_id uuid not null references admin.admin_user (id) on delete cascade,
    issuer text not null,
    subject text not null,
    email_at_provider text null,
    provider_username text null,
    created_at timestamptz not null,
    last_login_at timestamptz not null,
    unique (issuer, subject)
);

create index if not exists idx_admin_user_identity_user_id on admin.admin_user_identity (user_id);
