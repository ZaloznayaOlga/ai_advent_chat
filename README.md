# AI Chat

Android-приложение для общения с ИИ через чат с поддержкой нескольких AI провайдеров (DeepSeek, OpenAI).

## Возможности

### Чат
- Чат с ИИ через DeepSeek, OpenAI или HuggingFace API
- **Метаданные ответа** — под каждым сообщением ИИ отображается: время ответа, токены (input/output), стоимость
- **Расчёт стоимости** — автоматический расчёт для DeepSeek ($0.28/1M input, $0.42/1M output), остальные провайдеры — free
- **Копирование сообщений** — долгое нажатие копирует текст в буфер обмена
- **Очистка истории** — кнопка очистки чата в верхней панели
- Информативные сообщения об ошибках на русском языке

### Настройки AI
- **Температура** — настройка креативности ответов (0.0 - 1.5)
- **Стиль общения** — общие ответы или с уточняющими вопросами

### Формат ответов
- **Текст** — простой текстовый ответ
- **JSON** — структурированный ответ с метаданными (дата, время, тема, теги, ссылки)
- **XML** — структурированный ответ в XML формате
- **Просмотр исходного кода** — с подсветкой синтаксиса
- Копирование JSON/XML в буфер обмена

### Системный промпт
- **По умолчанию** — встроенные промпты для каждого формата
- **Пользовательский** — возможность задать свой системный промпт

## Скриншот

Приложение представляет собой экран чата с:
- Заголовком "AI Chat" и кнопками настроек/очистки в верхней части
- Областью сообщений с форматированными ответами
- При JSON формате: секции Дата, Время, Язык, Тема, Вопрос, Ответ, Теги, Ссылки
- Полем ввода и кнопкой отправки внизу

## Настройка API ключей

Добавьте необходимые API ключи в файл `local.properties` в корне проекта:

```properties
# DeepSeek API (https://platform.deepseek.com/)
DEEPSEEK_API_KEY=ваш_deepseek_ключ

# OpenAI API (https://platform.openai.com/) — опционально
OPENAI_API_KEY=ваш_openai_ключ

# HuggingFace API (https://huggingface.co/settings/tokens) — опционально
HUGGINGFACE_API_KEY=ваш_huggingface_ключ
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
│   ├── system-prompt.txt              # Основной системный промпт
│   ├── system-prompt-default.txt      # Промпт по умолчанию
│   ├── system-prompt-text.txt         # Промпт для текстового формата
│   ├── system-prompt-json.txt         # Промпт для JSON формата
│   ├── system-prompt-xml.txt          # Промпт для XML формата
│   └── system-prompt-with-questions.txt # Промпт с уточняющими вопросами
│
└── java/com/olgaz/aichat/
    ├── data/                           # Data Layer
    │   ├── provider/
    │   │   └── SystemPromptProviderImpl.kt  # Загрузка промпта из assets
    │   ├── remote/
    │   │   ├── api/
    │   │   │   └── ChatApi.kt          # Унифицированный Retrofit API
    │   │   ├── dto/
    │   │   │   ├── AiResponseJsonDto.kt # DTO для JSON ответа ИИ
    │   │   │   ├── ChatRequestDto.kt   # Request DTO с temperature
    │   │   │   └── ChatResponseDto.kt  # Response DTO
    │   │   └── logging/
    │   │       └── PrettyJsonLogger.kt # Форматированный логгер JSON
    │   └── repository/
    │       └── ChatRepositoryImpl.kt   # Repository + JSON/XML parsing
    │
    ├── domain/                         # Domain Layer
    │   ├── model/
    │   │   ├── Message.kt              # Domain model + MessageJsonData + MessageMetadata
    │   │   └── ChatSettings.kt         # Настройки: провайдер, модель, температура
    │   ├── provider/
    │   │   └── SystemPromptProvider.kt # Интерфейс провайдера промпта
    │   ├── repository/
    │   │   └── ChatRepository.kt       # Repository interface
    │   └── usecase/
    │       └── SendMessageUseCase.kt   # Business logic
    │
    ├── presentation/                   # Presentation Layer
    │   └── chat/
    │       ├── ChatScreen.kt           # Compose UI + copy on long press
    │       ├── ChatViewModel.kt        # ViewModel + clear history
    │       ├── ChatUiState.kt          # UI State
    │       └── SettingsDialog.kt       # Диалог настроек чата
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

## Форматы ответов ИИ

Приложение поддерживает три формата ответов:

### Текстовый формат
Простой текстовый ответ без структурирования.

### JSON формат

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

### XML формат

```xml
<response>
  <datetime>2026-01-13T22:42:00</datetime>
  <topic>Тема вопроса</topic>
  <question>Исходный вопрос пользователя</question>
  <answer>Подробный ответ с форматированием</answer>
  <tags>
    <tag>tag1</tag>
    <tag>tag2</tag>
  </tags>
  <links>
    <link>https://example.com</link>
  </links>
  <language>ru</language>
</response>
```

### Отображение структурированных ответов в UI

Ответ парсится и отображается в виде секций с рамками:
- **Дата** — форматированная дата (например, "13 января 2026")
- **Время** — время ответа
- **Язык** — код языка (RU, EN, и т.д.)
- **Тема** — краткая тема вопроса
- **Вопрос** — исходный вопрос
- **Ответ** — основной текст ответа
- **Теги** — релевантные теги в виде чипсов
- **Ссылки** — кликабельные ссылки
- **JSON/XML** — кнопка для просмотра исходного кода

### Просмотр исходного кода

При нажатии на кнопку "Показать исходный JSON/XML" открывается диалог с:
- Подсветкой синтаксиса
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

## Поддерживаемые AI провайдеры

### DeepSeek
- **Base URL:** `https://api.deepseek.com/`
- **Endpoint:** `POST /chat/completions`
- **Модели:** `deepseek-chat`, `deepseek-reasoner`

### OpenAI
- **Base URL:** `https://api.openai.com/v1/`
- **Endpoint:** `POST /chat/completions`
- **Модели:** `gpt-4o`, `gpt-4o-mini`, `o1-preview`, `o1-mini`

### HuggingFace
- **Base URL:** `https://router.huggingface.co/v1/`
- **Endpoint:** `POST /chat/completions`
- **Модели:** `Qwen/Qwen2.5-72B-Instruct`, `meta-llama/Llama-3.2-3B-Instruct`

## Автор

**Ольга Залозная**

## Лицензия

Этот проект создан для образовательных целей.