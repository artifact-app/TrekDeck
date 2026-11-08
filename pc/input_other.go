//go:build !windows

package main

// Заглушки для сборки/тестов вне Windows: только пишут в лог.

func pressCombo(codes []uint16)  { logf("[тест] key %v", codes) }
func typeText(s string)          { logf("[тест] type %q", s) }
func mouseMoveRel(dx, dy int)    { logf("[тест] move %d,%d", dx, dy) }
func mouseClick(b string, n int) { logf("[тест] click %s x%d", b, n) }
func mouseScroll(d int)          { logf("[тест] scroll %d", d) }
func openTarget(t string)        { logf("[тест] open %s", t) }
func startCmd(a []string)        { logf("[тест] start %v", a) }
func platformInit()              {}
