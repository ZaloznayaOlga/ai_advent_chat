# AI Chat

Android-приложение для общения с ИИ через чат, использующее DeepSeek API.

## Скриншот

Приложение представляет собой экран чата с:
- Заголовком "AI Chat" в верхней части
- Областью сообщений на весь экран
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
app/src/main/java/com/olgaz/aichat/
├── data/                           # Data Layer
│   ├── remote/
│   │   ├── api/
│   │   │   └── DeepSeekApi.kt      # Retrofit API interface
│   │   └── dto/
│   │       ├── ChatRequestDto.kt   # Request DTO
│   │       └── ChatResponseDto.kt  # Response DTO
│   └── repository/
│       └── ChatRepositoryImpl.kt   # Repository implementation
│
├── domain/                         # Domain Layer
│   ├── model/
│   │   └── Message.kt              # Domain model
│   ├── repository/
│   │   └── ChatRepository.kt       # Repository interface
│   └── usecase/
│       └── SendMessageUseCase.kt   # Business logic
│
├── presentation/                   # Presentation Layer
│   └── chat/
│       ├── ChatScreen.kt           # Compose UI
│       ├── ChatViewModel.kt        # ViewModel
│       └── ChatUiState.kt          # UI State
│
├── di/                             # Dependency Injection
│   ├── NetworkModule.kt            # Network dependencies
│   └── RepositoryModule.kt         # Repository bindings
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