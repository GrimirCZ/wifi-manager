package macutil

import "net"

func IsRandomizedMAC(value string) bool {
	parsed, err := net.ParseMAC(value)
	if err != nil || len(parsed) == 0 {
		return false
	}
	return parsed[0]&0x02 != 0
}
