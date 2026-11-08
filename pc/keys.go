package main

import (
	"fmt"
	"strings"
)

// Имена клавиш -> виртуальные коды Windows (VK_*)
var vkNames = map[string]uint16{
	"ctrl": 0x11, "control": 0x11, "shift": 0x10, "alt": 0x12, "win": 0x5B, "cmd": 0x5B,
	"enter": 0x0D, "return": 0x0D, "esc": 0x1B, "escape": 0x1B, "tab": 0x09, "space": 0x20,
	"backspace": 0x08, "delete": 0x2E, "del": 0x2E, "insert": 0x2D, "home": 0x24, "end": 0x23,
	"pgup": 0x21, "pageup": 0x21, "pgdn": 0x22, "pagedown": 0x22,
	"left": 0x25, "up": 0x26, "right": 0x27, "down": 0x28,
	"printscreen": 0x2C, "prtsc": 0x2C, "capslock": 0x14, "numlock": 0x90, "scrolllock": 0x91, "pause": 0x13,
	"plus": 0xBB, "minus": 0xBD, "comma": 0xBC, "period": 0xBE, "slash": 0xBF, "semicolon": 0xBA,
	"tilde": 0xC0, "grave": 0xC0,
	"playpause": 0xB3, "next": 0xB0, "prev": 0xB1, "stop": 0xB2,
	"volup": 0xAF, "voldown": 0xAE, "mute": 0xAD,
}

// vk разбирает имя клавиши ("ctrl", "a", "f5", "7" ...)
func vk(name string) (uint16, error) {
	n := strings.ToLower(strings.TrimSpace(name))
	if v, ok := vkNames[n]; ok {
		return v, nil
	}
	if len(n) == 1 {
		c := n[0]
		if c >= 'a' && c <= 'z' {
			return uint16(0x41 + (c - 'a')), nil
		}
		if c >= '0' && c <= '9' {
			return uint16(0x30 + (c - '0')), nil
		}
	}
	if strings.HasPrefix(n, "f") {
		var k int
		if _, err := fmt.Sscanf(n[1:], "%d", &k); err == nil && k >= 1 && k <= 24 {
			return uint16(0x70 + k - 1), nil
		}
	}
	return 0, fmt.Errorf("неизвестная клавиша %q", name)
}

// isExtended — клавиши, которым нужен флаг KEYEVENTF_EXTENDEDKEY
func isExtended(v uint16) bool {
	switch {
	case v >= 0x21 && v <= 0x28, v == 0x2D, v == 0x2E, v == 0x5B, v == 0x5C, v == 0x2C, v == 0x90:
		return true
	case v >= 0xAD && v <= 0xB3:
		return true
	}
	return false
}
