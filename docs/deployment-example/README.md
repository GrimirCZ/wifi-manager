# WiFi Manager deployment example

This directory contains anonymized configuration files for the deployment
scenario described in the thesis. The values are intentionally fictitious.

## Scenario

- Operating system: Rocky Linux
- Public application name: `wifi.example.test`
- WAN interface: `enp1s0`
- Captive VLAN interface: `enp2s0.30`
- Captive router address: `10.42.30.1`
- Captive subnet: `10.42.30.0/24`
- WiFi Manager installation directory: `/opt/wifimanager`
- Router agent installation directory: `/opt/wifimanager-routeragent`

IPv6 is represented by documentation addresses from `100::/64`. In this
example it is configured in nftables, but the prefix is treated as blackholed
upstream.

## Installation

Install the required operating system packages:

```sh
dnf install -y java-21-openjdk-headless postgresql-server postgresql nginx dnsmasq nftables make
```

Install the example files:

```sh
make install
make reload
```

The Makefile installs configuration into these locations:

- `/etc/wifimanager/wifimanager.env`
- `/etc/wifimanager/routeragent.env`
- `/etc/wifimanager/dnsmasq-wifimanager.conf`
- `/etc/systemd/system/wifimanager.service`
- `/etc/systemd/system/routeragent.service`
- `/etc/systemd/system/dnsmasq-wifimanager.service`
- `/etc/nginx/conf.d/wifimanager.conf`
- `/etc/nftables/main.nft`

Copy the application JAR to `/opt/wifimanager/wifimanager-app.jar` and the
router agent binary to `/opt/wifimanager-routeragent/routeragent`. Secrets in
the environment files are placeholders. Replace them before starting services.

## PostgreSQL setup

Initialize PostgreSQL on a fresh Rocky Linux installation:

```sh
postgresql-setup --initdb
systemctl enable --now postgresql
```

Create the database user and database:

```sh
sudo -u postgres psql
```

```sql
CREATE USER wifimanager WITH PASSWORD 'change-me';
CREATE DATABASE wifimanager OWNER wifimanager;
\c wifimanager
GRANT ALL PRIVILEGES ON SCHEMA public TO wifimanager;
```

Set the same password in `/etc/wifimanager/wifimanager.env`. The example keeps
the database on localhost and lets the application run Flyway migrations at
startup.

## Services

After adjusting secrets and copying binaries, enable the services:

```sh
make enable
```
