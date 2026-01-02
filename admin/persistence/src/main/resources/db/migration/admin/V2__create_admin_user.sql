create table if not exists admin.user_identity
(
    id            uuid primary key,
    user_id       uuid        not null,
    issuer        text        not null,
    subject       text        not null,
    email         text        not null,
    display_name  text        not null,
    picture_url   text        null,
    roles         text[]      not null default '{}',
    created_at    timestamptz not null,
    unique (issuer, subject)
);

create index if not exists idx_user_identity_user_id on admin.user_identity (user_id);
create index if not exists idx_user_identity_issuer_subject on admin.user_identity (issuer, subject);
