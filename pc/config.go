package main

import (
	"crypto/rand"
	"encoding/json"
	"fmt"
	"math/big"
	"os"
	"path/filepath"
	"sync"
)

type Button struct {
	Label  string `json:"label"`
	Icon   string `json:"icon"`
	Color  int    `json:"color"`
	Script string `json:"script"`
}

type Page struct {
	Name    string   `json:"name"`
	Buttons []Button `json:"buttons"`
}

type Config struct {
	Pin   string `json:"pin"`
	Pages []Page `json:"pages"`
}

const slots = 15 // 5 колонок x 3 ряда

var (
	cfg     Config
	cfgMu   sync.Mutex
	cfgPath string
)

func configPath() string {
	if exe, err := os.Executable(); err == nil {
		return filepath.Join(filepath.Dir(exe), "trekdeck-config.json")
	}
	return "trekdeck-config.json"
}

func normalize(pages []Page) []Page {
	for i := range pages {
		for len(pages[i].Buttons) < slots {
			pages[i].Buttons = append(pages[i].Buttons, Button{})
		}
		if len(pages[i].Buttons) > slots {
			pages[i].Buttons = pages[i].Buttons[:slots]
		}
	}
	return pages
}

func loadConfig() {
	cfgPath = configPath()
	if data, err := os.ReadFile(cfgPath); err == nil {
		if json.Unmarshal(data, &cfg) != nil {
			logf("не удалось прочитать %s, создаю заново", cfgPath)
			cfg = Config{}
		}
	}
	changed := false
	if len(cfg.Pin) != 6 {
		n, _ := rand.Int(rand.Reader, big.NewInt(1000000))
		cfg.Pin = fmt.Sprintf("%06d", n.Int64())
		changed = true
	}
	if len(cfg.Pages) == 0 {
		cfg.Pages = defaultPages()
		changed = true
	}
	cfg.Pages = normalize(cfg.Pages)
	if changed {
		saveConfig()
	}
}

func saveConfig() {
	cfgMu.Lock()
	defer cfgMu.Unlock()
	data, _ := json.MarshalIndent(cfg, "", "  ")
	tmp := cfgPath + ".tmp"
	if err := os.WriteFile(tmp, data, 0o600); err != nil {
		logf("не удалось сохранить настройки: %v", err)
		return
	}
	if err := os.Rename(tmp, cfgPath); err != nil {
		logf("не удалось сохранить настройки: %v", err)
	}
}

func snapshotPages() []Page {
	cfgMu.Lock()
	defer cfgMu.Unlock()
	data, _ := json.Marshal(cfg.Pages)
	var out []Page
	_ = json.Unmarshal(data, &out)
	return out
}
