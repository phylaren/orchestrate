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

**CI (GitHub Actions), прогін на push у гілку `andrii-e2e-browser` (коміт `de1d30e`):**

| Набір | Всього | Пройшло | Впало | Час тестів |
|---|---|---|---|---|
| Швидкі тести (`test`) | 590 | 590 | 0 | 31.7 с |
| Наскрізні браузерні (`e2eTest`) | 7 | 7 | 0 | 16.9 с |

Час кроків job (з GitHub Actions, `gh run view 40767103819 --json jobs`):

| Крок | Час |
|---|---|
| Set up job | 2 с |
| Check out repository | 1 с |
| Set up JDK 25 | 1 с |
| Set up Gradle | 3 с |
| Install Playwright Chromium with system dependencies | 1 хв 43 с |
| Run check (fast tests, then E2E tests) | 1 хв 52 с |
| Upload test reports | 2 с |
| **Разом job** | **≈ 3 хв 50 с** |

Загальний час job: 3 хв 50 с. Статус `check`: успішно. Прогін: https://github.com/phylaren/orchestrate/actions/runs/40767103819

![Перевірки Pull Request #17: усі перевірки пройшли](screenshots/pr-17-checks.png)

Pull Request: https://github.com/phylaren/orchestrate/pull/17

Розподіл E2E-тестів:

| Клас | Тестів |
|---|---|
| `AuthAndHouseholdE2EIT` | 1 |
| `ChoreParticipationE2EIT` | 1 |
| `ChorePermissionE2EIT` | 3 |
| `CoreFlowE2EIT` (тести Валерії) | 2 |

Трасування і скриншоти: за правилом завдання артефакти зберігаються лише при падінні. Оскільки падінь не було, `trace.zip` і `failure.png` не створювались, артефакт `e2e-artifacts` не завантажувався.

**Локально (macOS, без Docker):** `./gradlew test` дає 56 падінь через відсутність Docker, `e2eTest` не стартує без Docker. У CI ці обмеження не діють.

## 6. Обмеження

- Сторінки «Приєднатися» і «Позначити виконаним» з'явились у новій дошці `/households/{id}/board`; посилання на неї з існуючого списку домогосподарств не додано, бо це зміна старого шаблону.
- Форма створення домогосподарства доступна за адресою `/households/new`; посилання з навігації також не додано з тієї самої причини.
- Сценарій без права перевіряє GET сторінки створення обов'язку лише через UI (кнопки немає). Сам GET форми для мешканця без права відкривається (рендер), а заборона діє на POST. Закрити GET можна в існуючому контролері Валерії, що виходить за межі цього завдання.
