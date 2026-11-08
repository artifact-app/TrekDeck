package main

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

// runScript выполняет макрос: по одной команде в строке.
//
//	key ctrl+shift+m     — сочетание клавиш
//	type Привет!         — напечатать текст
//	open <путь|ссылка>   — открыть файл, папку, сайт, программу
//	start prog.exe арг   — запустить программу с аргументами
//	media playpause      — playpause | next | prev | stop | volup | voldown | mute
//	wait 500             — пауза в миллисекундах
func runScript(script string) {
	for _, line := range strings.Split(script, "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		cmd, arg := line, ""
		if i := strings.IndexAny(line, " \t"); i >= 0 {
			cmd, arg = line[:i], strings.TrimSpace(line[i+1:])
		}
		if err := runStep(strings.ToLower(cmd), arg); err != nil {
			logf("макрос: %v (строка %q)", err, line)
		}
	}
}

func runStep(cmd, arg string) error {
	switch cmd {
	case "key", "keys":
		parts := strings.Split(arg, "+")
		var codes []uint16
		for _, p := range parts {
			if strings.TrimSpace(p) == "" {
				continue
			}
			c, err := vk(p)
			if err != nil {
				return err
			}
			codes = append(codes, c)
		}
		if len(codes) == 0 {
			return fmt.Errorf("пустое сочетание")
		}
		pressCombo(codes)
	case "media":
		c, err := vk(arg)
		if err != nil {
			return err
		}
		pressCombo([]uint16{c})
	case "type", "text":
		typeText(arg)
	case "open":
		openTarget(arg)
	case "start", "run":
		args := strings.Fields(arg)
		if len(args) == 0 {
			return fmt.Errorf("start: не указана программа")
		}
		startCmd(args)
	case "wait", "sleep":
		ms, err := strconv.Atoi(arg)
		if err != nil || ms < 0 {
			return fmt.Errorf("wait: нужно число миллисекунд")
		}
		if ms > 30000 {
			ms = 30000
		}
		time.Sleep(time.Duration(ms) * time.Millisecond)
	default:
		return fmt.Errorf("неизвестная команда %q", cmd)
	}
	return nil
}
