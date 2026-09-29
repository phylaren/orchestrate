# Orchestrate — схема бази даних

Опис реляційної схеми: таблиці, стовпці, обмеження цілісності та правила роботи з БД.
Бізнес-правила, скінченні автомати й стратегії обміну — окремо в [`README.md`](README.md).

## Зміст

- [1. Схема й конфігурація](#1-схема-й-конфігурація)
- [2. Таблиці](#2-таблиці)
- [3. Стовпці](#3-стовпці)
- [4. Цілісність: зовнішні ключі, CHECK, індекси](#4-цілісність-зовнішні-ключі-check-індекси)
- [5. Правила БД](#5-правила-бд)

---

## 1. Схема й конфігурація

| Що | Де |
|---|---|
| DDL | `src/main/resources/db/migration/V1__init_schema.sql` |
| Міграції | Flyway (`spring.flyway.enabled=true`), каталог `classpath:db/migration` |
| База | H2 in-memory: `jdbc:h2:mem:orchestrate;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1` |
| Схема проти коду | `spring.jpa.hibernate.ddl-auto=validate` — Hibernate лише перевіряє відповідність, нічого не створює |
| Консоль | `spring.h2.console.enabled=true`, шлях `/h2-console` |
| SQL у журналі | `spring.jpa.show-sql=true`, `format_sql=true` |
| Сесія | `spring.jpa.open-in-view=false` — зв'язані дані збираються всередині транзакції сервісу |
| Іменування | `pk_*` — первинні ключі, `fk_*` — зовнішні, `uq_*` — унікальні, `chk_*` — CHECK, `idx_*` — індекси |

## 2. Таблиці

| № | Таблиця | Модуль | Призначення | Ключ |
|---|---|---|---|---|
| 1 | `users` | `user` | профілі користувачів | `id` UUID |
| 2 | `households` | `household` | домогосподарства | `id` UUID |
| 3 | `memberships` | `household` | членство: один рядок = один учасник одного дому | `(household_id, user_id)` |
| 4 | `membership_permissions` | `household` | набір прав учасника | `(household_id, user_id, permission)` |
| 5 | `invitation_codes` | `household` | активний код запрошення | `code` VARCHAR(32) |
| 6 | `chores` | `chore` | обов'язки дому та їхня періодичність | `id` UUID |
| 7 | `chore_participants` | `chore` | група ротації обов'язку | `(chore_id, user_id)` |
| 8 | `rotation_schedules` | `chore` | поточний стан ротації, один рядок на обов'язок | `chore_id` |
| 9 | `rotation_order` | `chore` | впорядкований `baseOrder` ротації | `(chore_id, position)` |
| 10 | `chore_completions` | `chore` | факти виконання та підтвердження | `id` UUID |
| 11 | `swap_requests` | `swap` | запити на обмін чергою | `id` UUID |
| 12 | `scheduled_swaps` | `swap` / `chore` | відкладений обмін для `TEMPORARY`, застосовується при `advance` | `id` UUID |

## 3. Стовпці

| Таблиця | Колонка | Тип | Null | Примітка |
|---|---|---|---|---|
| `users` | `id` | `UUID` | NOT NULL | первинний ключ |
| `users` | `display_name` | `VARCHAR(100)` | NOT NULL | ім'я для показу |
| `users` | `email` | `VARCHAR(254)` | NOT NULL | `uq_users_email`, зберігається в нижньому регістрі |
| `users` | `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент реєстрації |
| `households` | `id` | `UUID` | NOT NULL | первинний ключ |
| `households` | `name` | `VARCHAR(120)` | NOT NULL | назва дому |
| `households` | `owner_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `households` | `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент створення |
| `memberships` | `household_id` | `UUID` | NOT NULL | частина PK, FK → `households.id`, `CASCADE` |
| `memberships` | `user_id` | `UUID` | NOT NULL | частина PK, FK → `users.id`, `CASCADE` |
| `memberships` | `joined_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | коли учасник приєднався |
| `membership_permissions` | `household_id` | `UUID` | NOT NULL | частина PK, входить у складений FK → `memberships` |
| `membership_permissions` | `user_id` | `UUID` | NOT NULL | частина PK, входить у складений FK → `memberships` |
| `membership_permissions` | `permission` | `VARCHAR(50)` | NOT NULL | частина PK; `MANAGE_CHORES`, `CONFIRM_COMPLETIONS`, `INVITE_MEMBERS`, `MANAGE_MEMBERS` |
| `invitation_codes` | `code` | `VARCHAR(32)` | NOT NULL | первинний ключ |
| `invitation_codes` | `household_id` | `UUID` | NOT NULL | `uq_invitation_codes_household` — один активний код на дім |
| `invitation_codes` | `created_by_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `invitation_codes` | `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент генерації |
| `invitation_codes` | `expires_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | кінець строку дії |
| `chores` | `id` | `UUID` | NOT NULL | первинний ключ |
| `chores` | `household_id` | `UUID` | NOT NULL | FK → `households.id`, `CASCADE` |
| `chores` | `name` | `VARCHAR(120)` | NOT NULL | назва обов'язку |
| `chores` | `description` | `VARCHAR(1000)` | NULL | опис |
| `chores` | `recurrence_days` | `INT` | NOT NULL | `chk_chores_recurrence`: 1…365 |
| `chores` | `requires_confirmation` | `BOOLEAN` | NOT NULL | чи потребує підтвердження виконання |
| `chores` | `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент створення |
| `chore_participants` | `chore_id` | `UUID` | NOT NULL | частина PK, FK → `chores.id`, `CASCADE` |
| `chore_participants` | `user_id` | `UUID` | NOT NULL | частина PK, FK → `users.id`, `CASCADE` |
| `chore_participants` | `joined_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | коли учасник приєднався до групи |
| `chore_participants` | `added_by_admin` | `BOOLEAN` | NOT NULL | `DEFAULT FALSE`; додано адміністратором, а не самим учасником |
| `rotation_schedules` | `chore_id` | `UUID` | NOT NULL | первинний ключ, FK → `chores.id`, `CASCADE` |
| `rotation_schedules` | `current_index` | `INT` | NOT NULL | `DEFAULT 0`, `chk_rotation_schedules_index`: >= 0 |
| `rotation_schedules` | `current_cycle_number` | `INT` | NOT NULL | `DEFAULT 0`, `chk_rotation_schedules_cycle`: >= 0 |
| `rotation_schedules` | `cycle_started_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | початок поточного циклу |
| `rotation_order` | `chore_id` | `UUID` | NOT NULL | частина PK, FK → `rotation_schedules.chore_id`, `CASCADE` |
| `rotation_order` | `position` | `INT` | NOT NULL | частина PK; 0-based позиція в `baseOrder` |
| `rotation_order` | `user_id` | `UUID` | NOT NULL | FK → `users.id`, `CASCADE`; `uq_rotation_order_user` |
| `chore_completions` | `id` | `UUID` | NOT NULL | первинний ключ |
| `chore_completions` | `chore_id` | `UUID` | NOT NULL | FK → `chores.id`, `CASCADE` |
| `chore_completions` | `completed_by_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `chore_completions` | `completed_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент відмітки виконання |
| `chore_completions` | `status` | `VARCHAR(20)` | NOT NULL | `chk_chore_completions_status`: `NOT_REQUIRED`, `PENDING`, `CONFIRMED`, `REJECTED` |
| `chore_completions` | `confirmed_by_user_id` | `UUID` | NULL | FK → `users.id`, `RESTRICT`; заповнюється при `CONFIRMED` / `REJECTED` |
| `chore_completions` | `confirmed_at` | `TIMESTAMP WITH TIME ZONE` | NULL | `chk_chore_completions_confirmation`: разом із `confirmed_by_user_id` |
| `swap_requests` | `id` | `UUID` | NOT NULL | первинний ключ |
| `swap_requests` | `chore_id` | `UUID` | NOT NULL | FK → `chores.id`, `CASCADE` |
| `swap_requests` | `initiator_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `swap_requests` | `receiver_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `swap_requests` | `status` | `VARCHAR(20)` | NOT NULL | `chk_swap_requests_status`: `PENDING`, `ACCEPTED`, `REJECTED` |
| `swap_requests` | `swap_type` | `VARCHAR(20)` | NOT NULL | `chk_swap_requests_swap_type`: `PERMANENT`, `TEMPORARY` |
| `swap_requests` | `cycle_number` | `INT` | NULL | `chk_swap_requests_cycle_number`: обов'язковий для `TEMPORARY`, заборонений для `PERMANENT` |
| `swap_requests` | `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | NOT NULL | момент створення; у коді — `LocalDateTime` |
| `scheduled_swaps` | `id` | `UUID` | NOT NULL | первинний ключ |
| `scheduled_swaps` | `chore_id` | `UUID` | NOT NULL | FK → `chores.id`, `CASCADE` |
| `scheduled_swaps` | `from_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `scheduled_swaps` | `to_user_id` | `UUID` | NOT NULL | FK → `users.id`, `RESTRICT` |
| `scheduled_swaps` | `cycle_number` | `INT` | NOT NULL | цикл, у якому обмін застосовується |
| `scheduled_swaps` | `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | момент створення |

## 4. Цілісність: зовнішні ключі, CHECK, індекси

### Зовнішні ключі

| Таблиця.колонка | Ціль | `ON DELETE` | Причина |
|---|---|---|---|
| `households.owner_id` | `users.id` | `RESTRICT` | власника не можна видалити, поки існує дім |
| `memberships.household_id` | `households.id` | `CASCADE` | дім зникає разом із членствами |
| `memberships.user_id` | `users.id` | `CASCADE` | користувач зникає разом зі своїми членствами |
| `membership_permissions.(household_id, user_id)` | `memberships` | `CASCADE` | права не існують без членства |
| `invitation_codes.household_id` | `households.id` | `CASCADE` | код зникає разом із домом |
| `invitation_codes.created_by_user_id` | `users.id` | `RESTRICT` | автор коду має існувати |
| `chores.household_id` | `households.id` | `CASCADE` | обов'язки належать дому |
| `chore_participants.chore_id` | `chores.id` | `CASCADE` | участь не існує без обов'язку |
| `chore_participants.user_id` | `users.id` | `CASCADE` | участь не існує без користувача |
| `rotation_schedules.chore_id` | `chores.id` | `CASCADE` | розклад належить обов'язку |
| `rotation_order.chore_id` | `rotation_schedules.chore_id` | `CASCADE` | порядок належить розкладу |
| `rotation_order.user_id` | `users.id` | `CASCADE` | у порядку лише наявні користувачі |
| `chore_completions.chore_id` | `chores.id` | `CASCADE` | історія виконань належить обов'язку |
| `chore_completions.completed_by_user_id` | `users.id` | `RESTRICT` | виконавець має існувати — історія не «пливе» |
| `chore_completions.confirmed_by_user_id` | `users.id` | `RESTRICT` | підтверджувач має існувати |
| `swap_requests.chore_id` | `chores.id` | `CASCADE` | запити не існують без обов'язку |
| `swap_requests.initiator_user_id` | `users.id` | `RESTRICT` | ініціатор має існувати |
| `swap_requests.receiver_user_id` | `users.id` | `RESTRICT` | отримувач має існувати |
| `scheduled_swaps.chore_id` | `chores.id` | `CASCADE` | відкладений обмін належить обов'язку |
| `scheduled_swaps.from_user_id` / `to_user_id` | `users.id` | `RESTRICT` | обмін лишається в БД і ігнорується, а не зникає тихо |

### CHECK-обмеження

| Обмеження | Умова |
|---|---|
| `chk_chores_recurrence` | `recurrence_days BETWEEN 1 AND 365` |
| `chk_rotation_schedules_index` | `current_index >= 0` |
| `chk_rotation_schedules_cycle` | `current_cycle_number >= 0` |
| `chk_chore_completions_status` | `status IN ('NOT_REQUIRED','PENDING','CONFIRMED','REJECTED')` |
| `chk_chore_completions_confirmation` | `confirmed_by_user_id` і `confirmed_at` — або обидва заповнені, або обидва `NULL` |
| `chk_swap_requests_status` | `status IN ('PENDING','ACCEPTED','REJECTED')` |
| `chk_swap_requests_swap_type` | `swap_type IN ('PERMANENT','TEMPORARY')` |
| `chk_swap_requests_initiator_receiver` | `initiator_user_id <> receiver_user_id` |
| `chk_swap_requests_cycle_number` | `TEMPORARY` → `cycle_number IS NOT NULL`; `PERMANENT` → `cycle_number IS NULL` |

### Унікальність та індекси

| Об'єкт | Що забезпечує |
|---|---|
| `uq_users_email` | email унікальний |
| `uq_invitation_codes_household` | один активний код на дім |
| `uq_rotation_order_user` | користувач не дублюється в ротації |
| `idx_chores_household` | вибірка обов'язків дому |
| `idx_chore_completions_chore` | вибірка історії виконань |
| `idx_swap_requests_chore` | вибірка запитів обов'язку |
| `idx_swap_requests_pending` | перевірка дублю `PENDING` за парою учасників |
| `idx_scheduled_swaps_chore_cycle` | пошук і очищення відкладених обмінів за циклом |
| `idx_memberships_user` | доми користувача |

## 5. Правила БД

| № | Правило |
|---|---|
| BR-D1 | Первинні ключі — `UUID`, значення формує застосунок; у схемі немає `DEFAULT` для `id`, тому вставка рядка без `id` не передбачена. |
| BR-D2 | Значення переліків зберігаються **рядками** (`status`, `swap_type`, `permission`) і обмежені `CHECK`. Застосунок зобов'язаний писати рядкові значення; числові коди (`ORDINAL`) порушили б `CHECK`. |
| BR-D3 | `CHECK` — остання лінія оборони: сервіс валідує правило й повертає зрозумілу помилку, БД відхиляє неузгоджений рядок незалежно від сервісу. |
| BR-D4 | Політика `ON DELETE`: `CASCADE` — коли рядок є частиною цілого (`memberships` → дім, `chore_*` → обов'язок, `rotation_order` → розклад, `swap_requests`/`scheduled_swaps` → обов'язок); `RESTRICT` — коли посилання є історичним або довідковим (власник дому, автор коду, виконавець і підтверджувач виконання, учасники обміну). |
| BR-D5 | `UNIQUE` там, де бізнес-правило вимагає «рівно один активний»: `uq_invitation_codes_household`. Для пари «обов'язок + ініціатор + отримувач» `UNIQUE` свідомо немає — обмеження стосується лише статусу `PENDING` і забезпечується застосунком разом з `idx_swap_requests_pending`. |
| BR-D6 | Таблиці-зв'язки мають складений первинний ключ: `memberships`, `membership_permissions`, `chore_participants`, `rotation_order`. |
| BR-D7 | Часові типи: `TIMESTAMP WITH TIME ZONE` (`Instant` у коді) для всіх таблиць, крім `swap_requests.created_at` — там `TIMESTAMP WITHOUT TIME ZONE` (`LocalDateTime`). Узгодження типів можливе лише разом зі зміною відповідної доменної моделі. |
| BR-D8 | Схема не використовує діалектних типів понад `UUID`, `VARCHAR`, `INT`, `BOOLEAN`, `TIMESTAMP`, тому переноситься на PostgreSQL без змін. |
| BR-D9 | Зміна схеми — тільки новою міграцією (`V2__*.sql`); застосований файл не редагується. `ddl-auto=validate` нічого не створює і не змінює — розбіжність схеми й сутностей валить старт застосунку. |
| BR-D10 | Колонок версій (`@Version`) немає, тому конкурентні оновлення того самого рядка на рівні БД не захищені. |
| BR-D11 | Крос-модульні зовнішні ключі вимагають наявних рядків у цільових таблицях: `user`, `household` і `chore` мають бути заповнені в БД раніше за `swap_requests` і `scheduled_swaps`, інакше вставка відхиляється `FK`-перевіркою. |
