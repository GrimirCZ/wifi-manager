PREFIX ?= /opt
ETC_DIR ?= /etc/wifimanager
SYSTEMD_DIR ?= /etc/systemd/system
NGINX_DIR ?= /etc/nginx/conf.d
NFT_DIR ?= /etc/nftables
NFTABLES_SERVICE_CONF ?= /etc/sysconfig/nftables.conf

APP_DIR ?= $(PREFIX)/wifimanager
AGENT_DIR ?= $(PREFIX)/wifimanager-routeragent

.PHONY: install reload enable

install:
	install -d -m 0755 $(APP_DIR) $(AGENT_DIR)
	install -d -m 0750 $(ETC_DIR)
	install -d -m 0755 $(SYSTEMD_DIR) $(NGINX_DIR) $(NFT_DIR)
	install -d -o dnsmasq -g dnsmasq -m 0755 /var/lib/dnsmasq
	install -m 0640 wifimanager.env $(ETC_DIR)/wifimanager.env
	install -m 0640 routeragent.env $(ETC_DIR)/routeragent.env
	install -m 0644 dnsmasq-wifimanager.conf $(ETC_DIR)/dnsmasq-wifimanager.conf
	install -m 0644 wifimanager.service $(SYSTEMD_DIR)/wifimanager.service
	install -m 0644 routeragent.service $(SYSTEMD_DIR)/routeragent.service
	install -m 0644 dnsmasq-wifimanager.service $(SYSTEMD_DIR)/dnsmasq-wifimanager.service
	install -m 0644 nginx-wifimanager.conf $(NGINX_DIR)/wifimanager.conf
	install -m 0755 nftables-wifimanager.nft $(NFT_DIR)/main.nft
	install -m 0644 nftables.conf $(NFTABLES_SERVICE_CONF)

reload:
	systemctl daemon-reload
	nft -c -f $(NFT_DIR)/main.nft
	systemctl reload-or-restart nginx

enable:
	systemctl enable --now nftables
	systemctl enable --now dnsmasq-wifimanager
	systemctl enable --now wifimanager
	systemctl enable --now routeragent
