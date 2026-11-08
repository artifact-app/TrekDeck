package main

import (
	"bufio"
	"crypto/subtle"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"strings"
	"sync"
	"time"
)

const (
	tcpPort = 47600
	udpPort = 47601
)

var (
	logMu sync.Mutex
	runMu sync.Mutex
)

func logf(format string, a ...any) {
	logMu.Lock()
	defer logMu.Unlock()
	fmt.Printf("[%s] %s\n", time.Now().Format("15:04:05"), fmt.Sprintf(format, a...))
}

type Msg struct {
	T      string  `json:"t"`
	Pin    string  `json:"pin,omitempty"`
	Script string  `json:"script,omitempty"`
	DX     float64 `json:"dx,omitempty"`
	DY     float64 `json:"dy,omitempty"`
	B      string  `json:"b,omitempty"`
	N      int     `json:"n,omitempty"`
	Pages  []Page  `json:"pages,omitempty"`
}

func hostName() string {
	h, err := os.Hostname()
	if err != nil || h == "" {
		return "ПК"
	}
	return h
}

func localIPs() []string {
	var ips []string
	ifs, _ := net.Interfaces()
	for _, ifc := range ifs {
		if ifc.Flags&net.FlagUp == 0 || ifc.Flags&net.FlagLoopback != 0 {
			continue
		}
		addrs, _ := ifc.Addrs()
		for _, a := range addrs {
			if ipn, ok := a.(*net.IPNet); ok {
				if ip4 := ipn.IP.To4(); ip4 != nil && !ip4.IsLinkLocalUnicast() {
					ips = append(ips, ip4.String())
				}
			}
		}
	}
	return ips
}

func writeJSON(c net.Conn, v any) {
	data, _ := json.Marshal(v)
	c.SetWriteDeadline(time.Now().Add(5 * time.Second))
	c.Write(append(data, '\n'))
}

func clamp(v, lim float64) float64 {
	if v > lim {
		return lim
	}
	if v < -lim {
		return -lim
	}
	return v
}

func handle(c net.Conn) {
	defer c.Close()
	remote := c.RemoteAddr().String()
	sc := bufio.NewScanner(c)
	sc.Buffer(make([]byte, 64*1024), 4<<20)

	c.SetReadDeadline(time.Now().Add(10 * time.Second))
	if !sc.Scan() {
		return
	}
	var hello Msg
	if json.Unmarshal(sc.Bytes(), &hello) != nil || hello.T != "hello" ||
		subtle.ConstantTimeCompare([]byte(hello.Pin), []byte(cfg.Pin)) != 1 {
		logf("отказ в доступе: %s (неверный PIN)", remote)
		time.Sleep(1500 * time.Millisecond)
		writeJSON(c, map[string]any{"t": "err", "msg": "Неверный PIN"})
		return
	}
	logf("телефон подключён: %s", remote)
	writeJSON(c, map[string]any{"t": "ok", "name": hostName(), "pages": snapshotPages()})

	for {
		c.SetReadDeadline(time.Now().Add(60 * time.Second))
		if !sc.Scan() {
			break
		}
		var m Msg
		if json.Unmarshal(sc.Bytes(), &m) != nil {
			continue
		}
		switch m.T {
		case "ping":
			writeJSON(c, map[string]string{"t": "pong"})
		case "run":
			script := m.Script
			go func() {
				runMu.Lock()
				defer runMu.Unlock()
				runScript(script)
			}()
		case "mouse":
			mouseMoveRel(int(clamp(m.DX, 2000)), int(clamp(m.DY, 2000)))
		case "click":
			n := m.N
			if n < 1 {
				n = 1
			}
			if n > 3 {
				n = 3
			}
			mouseClick(m.B, n)
		case "scroll":
			mouseScroll(int(clamp(m.DY, 3000)))
		case "save":
			cfgMu.Lock()
			cfg.Pages = normalize(m.Pages)
			cfgMu.Unlock()
			saveConfig()
			logf("раскладки сохранены (%d стр.)", len(m.Pages))
		}
	}
	logf("телефон отключён: %s", remote)
}

func discovery() {
	pc, err := net.ListenPacket("udp4", fmt.Sprintf(":%d", udpPort))
	if err != nil {
		logf("автопоиск недоступен: %v", err)
		return
	}
	buf := make([]byte, 256)
	for {
		n, addr, err := pc.ReadFrom(buf)
		if err != nil {
			return
		}
		if strings.TrimSpace(string(buf[:n])) == "TREKDECK?" {
			pc.WriteTo([]byte(fmt.Sprintf("TREKDECK|%s|%d", hostName(), tcpPort)), addr)
		}
	}
}

func main() {
	platformInit()
	loadConfig()

	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", tcpPort))
	if err != nil {
		fmt.Printf("Не удалось открыть порт %d: %v\nВозможно, ТрекДек уже запущен.\n", tcpPort, err)
		time.Sleep(10 * time.Second)
		os.Exit(1)
	}
	go discovery()

	fmt.Println("==========================================")
	fmt.Println("   ТрекДек — сервер для ПК запущен")
	fmt.Println("==========================================")
	fmt.Printf("   PIN телефона:   %s\n", cfg.Pin)
	for _, ip := range localIPs() {
		fmt.Printf("   Адрес ПК:       %s\n", ip)
	}
	fmt.Println("------------------------------------------")
	fmt.Println("   Открой приложение на телефоне (тот же Wi‑Fi):")
	fmt.Println("   ПК найдётся сам, введи PIN. Окно не закрывай.")
	fmt.Printf("   Настройки: %s\n", cfgPath)
	fmt.Println("==========================================")

	for {
		c, err := ln.Accept()
		if err != nil {
			continue
		}
		go handle(c)
	}
}
