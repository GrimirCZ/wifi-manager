alter table admin.allowed_mac
    add column if not exists hostname text null;
