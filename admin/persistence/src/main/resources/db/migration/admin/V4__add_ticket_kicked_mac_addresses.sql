alter table admin.ticket
    add column if not exists kicked_mac_addresses text[] not null default '{}';
