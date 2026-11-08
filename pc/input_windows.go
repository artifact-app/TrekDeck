//go:build windows

package main

import (
	"os/exec"
	"syscall"
	"time"
	"unicode/utf16"
	"unsafe"
)

var (
	user32       = syscall.NewLazyDLL("user32.dll")
	kernel32     = syscall.NewLazyDLL("kernel32.dll")
	shell32      = syscall.NewLazyDLL("shell32.dll")
	procSendIn   = user32.NewProc("SendInput")
	procShellEx  = shell32.NewProc("ShellExecuteW")
	procGetStd   = kernel32.NewProc("GetStdHandle")
	procGetMode  = kernel32.NewProc("GetConsoleMode")
	procSetMode  = kernel32.NewProc("SetConsoleMode")
	procSetCP    = kernel32.NewProc("SetConsoleOutputCP")
	procSetTitle = kernel32.NewProc("SetConsoleTitleW")
)

const (
	inputMouse    = 0
	inputKeyboard = 1

	keyExtended = 0x0001
	keyUp       = 0x0002
	keyUnicode  = 0x0004

	mouseMove      = 0x0001
	mouseLeftDown  = 0x0002
	mouseLeftUp    = 0x0004
	mouseRightDown = 0x0008
	mouseRightUp   = 0x0010
	mouseMidDown   = 0x0020
	mouseMidUp     = 0x0040
	mouseWheel     = 0x0800
)

// обе структуры занимают 40 байт на amd64 (размер INPUT)
type mouseInput struct {
	typ   uint32
	_     uint32
	dx    int32
	dy    int32
	data  uint32
	flags uint32
	time  uint32
	_     uint32
	extra uintptr
}

type keyInput struct {
	typ   uint32
	_     uint32
	vk    uint16
	scan  uint16
	flags uint32
	time  uint32
	_     uint32
	extra uintptr
	_     [8]byte
}

func sendRaw(p unsafe.Pointer) {
	procSendIn.Call(1, uintptr(p), unsafe.Sizeof(keyInput{}))
}

func sendKey(v uint16, up bool) {
	var f uint32
	if up {
		f |= keyUp
	}
	if isExtended(v) {
		f |= keyExtended
	}
	in := keyInput{typ: inputKeyboard, vk: v, flags: f}
	sendRaw(unsafe.Pointer(&in))
}

func pressCombo(codes []uint16) {
	for _, c := range codes {
		sendKey(c, false)
		time.Sleep(8 * time.Millisecond)
	}
	time.Sleep(20 * time.Millisecond)
	for i := len(codes) - 1; i >= 0; i-- {
		sendKey(codes[i], true)
		time.Sleep(8 * time.Millisecond)
	}
}

func typeText(s string) {
	for _, r := range s {
		if r == '\n' {
			pressCombo([]uint16{0x0D})
			continue
		}
		for _, u := range utf16.Encode([]rune{r}) {
			d := keyInput{typ: inputKeyboard, scan: u, flags: keyUnicode}
			sendRaw(unsafe.Pointer(&d))
			up := keyInput{typ: inputKeyboard, scan: u, flags: keyUnicode | keyUp}
			sendRaw(unsafe.Pointer(&up))
		}
		time.Sleep(4 * time.Millisecond)
	}
}

func mouseSend(flags uint32, dx, dy int32, data uint32) {
	in := mouseInput{typ: inputMouse, dx: dx, dy: dy, data: data, flags: flags}
	sendRaw(unsafe.Pointer(&in))
}

func mouseMoveRel(dx, dy int) { mouseSend(mouseMove, int32(dx), int32(dy), 0) }

func mouseClick(button string, n int) {
	var down, up uint32
	switch button {
	case "right":
		down, up = mouseRightDown, mouseRightUp
	case "middle":
		down, up = mouseMidDown, mouseMidUp
	default:
		down, up = mouseLeftDown, mouseLeftUp
	}
	for i := 0; i < n; i++ {
		mouseSend(down, 0, 0, 0)
		time.Sleep(15 * time.Millisecond)
		mouseSend(up, 0, 0, 0)
		time.Sleep(25 * time.Millisecond)
	}
}

func mouseScroll(delta int) { mouseSend(mouseWheel, 0, 0, uint32(int32(delta))) }

func openTarget(t string) {
	verb, _ := syscall.UTF16PtrFromString("open")
	file, err := syscall.UTF16PtrFromString(t)
	if err != nil {
		return
	}
	procShellEx.Call(0, uintptr(unsafe.Pointer(verb)), uintptr(unsafe.Pointer(file)), 0, 0, 1)
}

func startCmd(args []string) {
	full := append([]string{"/C", "start", ""}, args...)
	c := exec.Command("cmd", full...)
	c.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
	_ = c.Start()
	go c.Wait()
}

// platformInit: русские буквы в консоли и отключение «заморозки» при клике по окну
func platformInit() {
	procSetCP.Call(65001)
	title, _ := syscall.UTF16PtrFromString("ТрекДек — сервер для ПК")
	procSetTitle.Call(uintptr(unsafe.Pointer(title)))
	h, _, _ := procGetStd.Call(^uintptr(10 - 1)) // STD_INPUT_HANDLE = -10
	var mode uint32
	if r, _, _ := procGetMode.Call(h, uintptr(unsafe.Pointer(&mode))); r != 0 {
		mode &^= 0x0040 // ENABLE_QUICK_EDIT_MODE
		mode |= 0x0080  // ENABLE_EXTENDED_FLAGS
		procSetMode.Call(h, uintptr(mode))
	}
}
