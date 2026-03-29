alter table if exists captive.network_user_device
    add column if not exists fingerprint_profile jsonb null,
    add column if not exists fingerprint_status text not null default 'NONE',
    add column if not exists fingerprint_verified_at timestamptz null,
    add column if not exists reauth_required_at timestamptz null;

alter table if exists captive.captive_device
    add column if not exists fingerprint_profile jsonb null,
    add column if not exists fingerprint_status text not null default 'NONE',
    add column if not exists fingerprint_verified_at timestamptz null,
    add column if not exists reauth_required_at timestamptz null;
