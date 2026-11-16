# S5 Achievements

Мод Minecraft Forge 1.20.1 (Java 17): достижения сервера «Департамент S5» в одной вкладке. Автор — faygoLover (Alex).
**С автором общаемся по-русски.** Мод `s5adv`, пакет `faygolover.s5adv`, MIT. Нужен только на сервере
(`displayTest=IGNORE_SERVER_VERSION`), клиент получает достижения ванильным пакетом.

## Состояние
- **26.10.2026, 1.0.0:** сделано по решениям Alex (разбор 24 пунктов). Проверено на dev-сервере и dev-клиенте:
  вкладка, скрытые не видны, всплывашка, чат, `/s5adv reload` без перезапуска (правка, новый файл, битый файл),
  шутка `/sex` — все 7 ответов, потом тишина. В сборку `S5 Packs` не добавлен.

## Устройство
- `mixin/ServerAdvancementManagerMixin` — в конце `apply` (старт, `/reload`) вызывает `AdvStore.install`.
- `AdvStore` — читает `config/s5adv/**.json` → `s5:<путь>`, блок `"s5"` → флажки, родитель вне s5 → `s5:root`,
  без criteria → `minecraft:impossible`. `reload(server)`: сохранить прогресс игроков → заменить наши в списке →
  `PlayerAdvancements.reload` (как ванильный `/reload`). Последняя исправная версия файла держится в памяти.
  Встроенные файлы (`s5adv_defaults/`) кладутся по одному разу, список в `config/s5adv/.defaults`.
- `S5Adv.onEarn` — флажок чата: сами шлём ванильное `chat.type.advancement.*` (gamerule не нужен).
- `S5Advancements` — API для других модов; `S5AdvCommand` — `/s5adv`; `SexCommand` — шутка (счётчик в `PlayerPersisted`).
- Формат файла — `docs/format.md` (по нему делает страницу сессия панели).

## Сборка и проверка
JDK 17: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.14.7-hotspot"`, `gradlew.bat build` → `build/libs`.
Dev: `run/server.properties` (порт 25611, RCON 25612, пароль `s5advtest`, плоский мир) → `gradlew runServer`;
клиент — `gradlew runClient -Pquickplay=127.0.0.1:25611 -Pusername=Dev` (через `localhost` повисает на IPv6).
Команды игроку — `execute as Dev run …` по RCON. В `run/config/s5adv` лежат тестовые `chat_test`, `toast_test`, `reload_new`.
