# S5 Achievements — указатель кода

Создано `python _family/tools/mapgen.py`, версия мода 1.0.0-1.20.1-forge.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 7 файлов, 524 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.s5adv` | 6 | 491 |
| `faygolover.s5adv.mixin` | 1 | 33 |

## 2. Классы

### `faygolover.s5adv`
- `AdvStore`, 236 — Наши достижения: файлы config/s5adv/**.json → id s5: .
- `ManagerAccess` (interface), 11 — Доступ к закрытым полям ServerAdvancementManager (реализует миксин).
- `S5Adv`, 43 — S5 Achievements — достижения сервера «Департамент S5» в одной вкладке.
- `S5AdvCommand`, 94 — /s5adv reload | list [игрок] | give | revoke — права 2.
- `S5Advancements`, 50 — Публичное API для других модов.
- `SexCommand`, 57 — Шутка: /sex anasvaldigor | archibald | Арчибальд, без прав.

### `faygolover.s5adv.mixin`
- `ServerAdvancementManagerMixin`, 33 — После загрузки достижений датапаков (старт сервера, /reload) в готовый список добавляются наши из config/s5adv.

Классов без описания: 0 из 7.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `AdvancementEvent.AdvancementEarnEvent` | `S5Adv.onEarn` |
| `RegisterCommandsEvent` | `S5Adv.onCommands` |

## 4. Сеть

Нет.

## 5. Команды

- `faygolover/s5adv/S5AdvCommand.java`: `s5adv` `reload` `list` `give` `revoke`
- `faygolover/s5adv/SexCommand.java`: `sex`

## 6. Данные

- свои файлы на диске: `AdvStore`
- getPersistentData: `SexCommand` ×2

## 7. Конфиги

Нет.

## 8. Что берёт из RP Core и что ему даёт

Не использует.

## 9. Связи с другими модами


Миксины (1):
- `ServerAdvancementManagerMixin` (mixins) → ServerAdvancementManager.class

## 10. Зарегистрированные id

Нет или регистрируются не строковыми литералами.
