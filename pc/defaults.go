package main

func b(label, icon string, color int, script string) Button {
	return Button{Label: label, Icon: icon, Color: color, Script: script}
}

// Цвета плиток: 0 синий, 1 циан, 2 индиго, 3 бирюзовый, 4 фиолетовый, 5 красный
func defaultPages() []Page {
	return []Page{
		{Name: "Работа", Buttons: []Button{
			b("Браузер", "🌐", 0, "open https://www.google.com"),
			b("Проводник", "📁", 1, "key win+e"),
			b("Заметки", "📝", 2, "start notepad.exe"),
			b("Калькулятор", "🧮", 3, "start calc.exe"),
			b("Скриншот", "📸", 1, "key win+shift+s"),
			b("Копировать", "📋", 0, "key ctrl+c"),
			b("Вставить", "📥", 0, "key ctrl+v"),
			b("Отмена", "↩️", 2, "key ctrl+z"),
			b("Рабочий стол", "🖥️", 1, "key win+d"),
			b("Диспетчер", "⚙️", 3, "key ctrl+shift+esc"),
			b("Блокировка", "🔒", 2, "key win+l"),
			b("Alt+Tab", "🔀", 0, "key alt+tab"),
			b("Закрыть", "❌", 5, "key alt+f4"),
			b("Найти", "🔎", 1, "key ctrl+f"),
			b("Поиск Win", "🔍", 3, "key win+s"),
		}},
		{Name: "Стрим", Buttons: []Button{
			b("Сцена 1", "🎬", 0, "key ctrl+alt+1"),
			b("Сцена 2", "🎬", 1, "key ctrl+alt+2"),
			b("Сцена 3", "🎬", 2, "key ctrl+alt+3"),
			b("Сцена 4", "🎬", 3, "key ctrl+alt+4"),
			b("Микрофон", "🎤", 5, "key ctrl+alt+m"),
			b("Запись", "🔴", 5, "key ctrl+alt+r"),
			b("Эфир", "📡", 0, "key ctrl+alt+s"),
			b("Камера", "📷", 1, "key ctrl+alt+c"),
			b("Чат", "💬", 2, "open https://www.twitch.tv"),
			b("Звук", "🔇", 3, "media mute"),
			b("Громче", "🔊", 0, "media volup"),
			b("Тише", "🔉", 1, "media voldown"),
			b("Game Bar", "🎮", 2, "key win+g"),
			b("Клип", "✂️", 3, "key win+alt+g"),
			b("Скриншот", "📸", 0, "key win+shift+s"),
		}},
		{Name: "Игры", Buttons: []Button{
			b("Alt+Tab", "🔀", 0, "key alt+tab"),
			b("Game Bar", "🎮", 1, "key win+g"),
			b("Запись", "🔴", 5, "key win+alt+r"),
			b("Скриншот", "📸", 2, "key win+alt+prtsc"),
			b("Steam", "🕹️", 3, "open steam://open/main"),
			b("Discord", "💬", 4, "open https://discord.com/app"),
			b("Звук", "🔇", 1, "media mute"),
			b("Громче", "🔊", 0, "media volup"),
			b("Тише", "🔉", 2, "media voldown"),
			b("Диспетчер", "⚙️", 3, "key ctrl+shift+esc"),
		}},
		{Name: "Медиа", Buttons: []Button{
			b("Пауза", "⏯️", 0, "media playpause"),
			b("Назад", "⏮️", 1, "media prev"),
			b("Вперёд", "⏭️", 1, "media next"),
			b("Стоп", "⏹️", 2, "media stop"),
			b("Звук", "🔇", 3, "media mute"),
			b("Громче", "🔊", 0, "media volup"),
			b("Тише", "🔉", 1, "media voldown"),
			b("YouTube", "▶️", 5, "open https://www.youtube.com"),
			b("Spotify", "🎵", 3, "start spotify.exe"),
		}},
	}
}
