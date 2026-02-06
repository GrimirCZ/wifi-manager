create table if not exists captive.allowed_mac
(
    device_mac text primary key references captive.captive_device (mac) on delete cascade,
    valid_until timestamptz null
);
