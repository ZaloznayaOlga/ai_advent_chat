# AI Chat

Android-приложение для общения с ИИ через чат, использующее DeepSeek API.

## Возможности

- Чат с ИИ через DeepSeek API
- **Структурированные ответы** — ИИ отвечает в формате JSON, который парсится и отображается в красивом виде
- Отображение метаданных ответа: дата, время, тема, язык
- Теги и ссылки в ответах
- **Просмотр исходного JSON** с подсветкой синтаксиса
- Копирование JSON в буфер обмена
- Информативные сообщения об ошибках на русском языке

## Скриншот

Приложение представляет собой экран чата с:
- Заголовком "AI Chat" в верхней части
- Областью сообщений с форматированными ответами
- Секциями: Дата, Время, Язык, Тема, Вопрос, Ответ, Теги, Ссылки, JSON
- Полем ввода и кнопкой отправки внизу

## Настройка API ключа

1. Получите API ключ на сайте [DeepSeek](https://platform.deepseek.com/)
2. Откройте файл `local.properties` в корне проекта
3. Добавьте или измените строку:
   ```
   DEEPSEEK_API_KEY=ваш_api_ключ_здесь
   ```

**Важно:** Файл `local.properties` не должен попадать в систему контроля версий (уже добавлен в `.gitignore`).

## Запуск приложения

### Через Android Studio
1. Откройте проект в Android Studio
2. Дождитесь синхронизации Gradle
3. Подключите устройство или запустите эмулятор
4. Нажмите Run (Shift+F10)

### Через командную строку
```bash
# Сборка debug APK
./gradlew assembleDebug

# Установка на подключённое устройство
./gradlew installDebug

# Запуск тестов
./gradlew test
```

## Архитектура проекта

Проект построен по принципам **Clean Architecture** с использованием паттерна **MVVM**.

```
app/src/main/
├── assets/
│   └── system-prompt.txt           # Системный промпт для JSON формата
│
└── java/com/olgaz/aichat/
    ├── data/                           # Data Layer
    │   ├── provider/
    │   │   └── SystemPromptProviderImpl.kt  # Загрузка промпта из assets
    │   ├── remote/
    │   │   ├── api/
    │   │   │   └── DeepSeekApi.kt      # Retrofit API interface
    │   │   └── dto/
    │   │       ├── AiResponseJsonDto.kt # DTO для JSON ответа ИИ
    │   │       ├── ChatRequestDto.kt   # Request DTO
    │   │       └── ChatResponseDto.kt  # Response DTO
    │   └── repository/
    │       └── ChatRepositoryImpl.kt   # Repository + JSON parsing
    │
    ├── domain/                         # Domain Layer
    │   ├── model/
    │   │   └── Message.kt              # Domain model + MessageJsonData
    │   ├── provider/
    │   │   └── SystemPromptProvider.kt # Интерфейс провайдера промпта
    │   ├── repository/
    │   │   └── ChatRepository.kt       # Repository interface
    │   └── usecase/
    │       └── SendMessageUseCase.kt   # Business logic
    │
    ├── presentation/                   # Presentation Layer
    │   └── chat/
    │       ├── ChatScreen.kt           # Compose UI + JSON viewer
    │       ├── ChatViewModel.kt        # ViewModel
    │       └── ChatUiState.kt          # UI State
    │
    ├── di/                             # Dependency Injection
    │   ├── NetworkModule.kt            # Network dependencies
    │   └── RepositoryModule.kt         # Repository + Provider bindings
    │
    ├── ui/theme/                       # Material3 Theme
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    │
    ├── AIChatApplication.kt            # Hilt Application
    └── MainActivity.kt                 # Entry point
```

### Слои архитектуры

- **Data Layer** — работа с сетью (Retrofit, OkHttp), DTO, реализация репозиториев
- **Domain Layer** — бизнес-логика, модели, интерфейсы репозиториев, use cases
- **Presentation Layer** — UI (Jetpack Compose), ViewModel, UI State

## Формат ответа ИИ

ИИ настроен отвечать в структурированном JSON формате:

```json
{
  "datetime": "2026-01-13T22:42:00",
  "topic": "Тема вопроса",
  "question": "Исходный вопрос пользователя",
  "answer": "Подробный ответ с форматированием",
  "tags": ["tag1", "tag2", "tag3"],
  "links": ["https://example.com"],
  "language": "ru"
}
```

### Отображение в UI

Ответ парсится и отображается в виде секций с рамками:
- **Дата** — форматированная дата (например, "13 января 2026")
- **Время** — время ответа
- **Язык** — код языка (RU, EN, и т.д.)
- **Тема** — краткая тема вопроса
- **Вопрос** — исходный вопрос
- **Ответ** — основной текст ответа
- **Теги** — релевантные теги в виде чипсов
- **Ссылки** — кликабельные ссылки
- **JSON** — кнопка для просмотра исходного JSON

### Просмотр JSON

При нажатии на кнопку "Показать исходный JSON" открывается диалог с:
- Подсветкой синтаксиса (ключи — голубые, значения — зелёные, скобки — белые)
- Прокруткой для длинных ответов
- Кнопкой копирования в буфер обмена

## Обработка ошибок

Приложение имеет информативную обработку ошибок с понятными сообщениями на русском языке:

| Ситуация | Сообщение пользователю |
|----------|------------------------|
| Неверный API ключ (401) | Ошибка доступа к API. Проверьте корректность API ключа! |
| Доступ запрещён (403) | Доступ запрещён. Проверьте права доступа API ключа. |
| Лимит запросов (429) | Слишком много запросов. Подождите немного и попробуйте снова. |
| Ошибка сервера (5xx) | Сервер временно недоступен. Попробуйте позже. |
| Нет интернета | Нет подключения к интернету |
| Таймаут соединения | Превышено время ожидания ответа. Попробуйте снова. |

## Используемые технологии

| Технология | Назначение |
|------------|------------|
| **Kotlin** | Язык программирования |
| **Jetpack Compose** | Декларативный UI фреймворк |
| **Material3** | Дизайн-система |
| **Hilt** | Dependency Injection |
| **Retrofit** | HTTP клиент |
| **OkHttp** | Сетевой слой |
| **Kotlin Serialization** | JSON сериализация |
| **Coroutines + Flow** | Асинхронность и реактивность |
| **ViewModel** | Управление UI состоянием |

## Конфигурация

| Параметр | Значение |
|----------|----------|
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |
| Hilt | 2.51.1 |

## API

Приложение использует DeepSeek Chat API:
- **Base URL:** `https://api.deepseek.com/`
- **Endpoint:** `POST /chat/completions`
- **Model:** `deepseek-chat`

## Автор

**Ольга Залозная**

## Лицензия

Этот проект создан для образовательных целей.