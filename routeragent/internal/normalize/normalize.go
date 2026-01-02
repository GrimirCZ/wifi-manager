package normalize

import (
	"net"
	"strings"
)

func IP(value string) (string, bool) {
	parsed := net.ParseIP(strings.TrimSpace(value))
	if parsed == nil {
		return "", false
	}
	if v4 := parsed.To4(); v4 != nil {
		return v4.String(), true
	}
	return parsed.String(), true
}

func MAC(value string) (string, bool) {
	parsed, err := net.ParseMAC(strings.TrimSpace(value))
	if err != nil {
		return "", false
	}
	return strings.ToLower(parsed.String()), true
}

func IPFromNet(ip net.IP) (string, bool) {
	if len(ip) == 0 {
		return "", false
	}
	if v4 := ip.To4(); v4 != nil {
		return v4.String(), true
	}
	return ip.String(), true
}

func MACFromNet(mac net.HardwareAddr) (string, bool) {
	if len(mac) == 0 {
		return "", false
	}
	return strings.ToLower(mac.String()), true
}
