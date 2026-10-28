# Звіт: браузерні (E2E) тести, розділення рівнів і CI

## 1. Структура тестів за рівнями

| Рівень | Імена класів | Запуск | Залежність |
|---|---|---|---|
| Модульні (швидкі) | `*Test` без Spring-контексту | `./gradlew test` | немає |
| Веб-зрізи | `*ControllerTest`, `*WebUiIntegrationTest`, `*WebErrorAdviceTest` (`@WebMvcTest`, MockMvc) | `./gradlew test` | немає |
| Інтеграційні | `*IntegrationTest`, `*ApiIntegrationTest`, `*PersistenceTest`, `*SmokeTest` | `./gradlew test` | Docker (Testcontainers, PostgreSQL) |
| Наскрізні браузерні (E2E) | `*E2EIT` у пакеті `e2e` і `integration` | `./gradlew e2eTest` | Docker, Chromium (Playwright) |

Розділення налаштовано в `build.gradle.kts` (нові рядки в кінці файлу):
- `test` виключає `**/*E2EIT*`;
- `e2eTest` включає лише `**/*E2EIT*`, передає каталог артефактів `build/e2e-artifacts`;
- `check` залежить від `e2eTest`, тож `./gradlew check` запускає все;
- `installPlaywright` завантажує Chromium з системними залежностями.

## 2. Браузерні сценарії (`src/test/java/genius/project/orchestrate/e2e`)

| Клас | Сценарій | Перевірка результату |
|---|---|---|
| `AuthAndHouseholdE2EIT` | Вхід → створення домогосподарства через форму | повідомлення про створення, заголовок дошки, рядок у `households` з власником (JdbcClient) |
| `ChoreParticipationE2EIT` | Мешканець приєднується до обов'язку і відмічає виконання | кнопка «Позначити виконаним» з'явилась, рядок у `chore_completions` з його id (JdbcClient) |
| `ChorePermissionE2EIT` | Мешканець без `MANAGE_CHORES` | кнопки створення немає; прямий POST з чинним CSRF-токеном дає 403; обов'язок не створено |
| `ChorePermissionE2EIT` (контроль) | Власник з правом | кнопка створення видна |

Сторінки (Page Object): `e2e/pages/LoginPage`, `HouseholdPages`, `ChorePages`.
Спільний життєвий цикл браузера: `e2e/BrowserTestSupport` (JUnit-розширення). Тест отримує `Page page` параметром.
Базовий клас з адресою застосунку й фікстурами: `e2e/BrowserE2ETestBase`.

## 3. Трасування і скриншоти при падінні

- Трасування (`trace.zip`) і знімок екрана (`failure.png`) записуються лише для тестів, що впали.
- Шлях: `build/e2e-artifacts/<КласТесту>/<назваМетоду>/`.
- Переглянути трасування: `npx playwright show-trace <шлях>/trace.zip` або https://trace.playwright.dev.
- У CI каталог `build/e2e-artifacts` завантажується як артефакт `e2e-artifacts` при падінні.

## 4. CI (GitHub Actions)

Файл `.github/workflows/ci.yml`:
1. JDK 25 (temurin), кеш Gradle.
2. `./gradlew installPlaywright`.
3. `./gradlew check`: швидкі тести, потім E2E.
4. Артефакти: `test-reports` (звіти й XML) завжди; `e2e-artifacts` при падінні.

Docker доступний на `ubuntu-latest`, тож Testcontainers працює без додаткових кроків.

## 5. Результати

**Локально (macOS, без Docker):**

| Набір | Всього | Пройшло | Не запустилось через Docker |
|---|---|---|---|
| `./gradlew test` | 590 | 534 | 56 (усі інтеграційні, Testcontainers) |
| `./gradlew e2eTest` | 7 | 0 | 7 (контекст не піднявся без Docker) |
| `./gradlew installPlaywright` | — | успішно | — |

Час: `./gradlew test` ≈ 16 с разом із компіляцією; сумарний час самих тестів ≈ 4 с.

**Повний прогін `./gradlew check` і скриншоти з трасуванням з'являться після першого запуску в CI**: локально Docker відсутній, тому тут їх не отримати. Після прогону звіт доповнюється таблицею результатів і знімками з артефактів.

## 6. Обмеження

- Сторінки «Приєднатися» і «Позначити виконаним» з'явились у новій дошці `/households/{id}/board`; посилання на неї з існуючого списку домогосподарств не додано, бо це зміна старого шаблону.
- Форма створення домогосподарства доступна за адресою `/households/new`; посилання з навігації також не додано з тієї самої причини.
- Сценарій без права перевіряє GET сторінки створення обов'язку лише через UI (кнопки немає). Сам GET форми для мешканця без права відкривається (рендер), а заборона діє на POST. Закрити GET можна в існуючому контролері Валерії, що виходить за межі цього завдання.
