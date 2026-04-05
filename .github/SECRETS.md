# Secrets для GitHub Actions

Settings → Secrets and variables → Actions

## Обязательные (конфиг приложения)

| Секрет | Описание | Дефолт |
|--------|----------|--------|
| `API_BASE_URL` | URL бэкенда | `https://openeljur-api.vercel.app` |
| `DEFAULT_SCHOOL_ID` | ID школы | `eljur` |

## Опциональные (подпись APK)

Без них APK будет unsigned (можно установить только через ADB или включив "Unknown sources").

| Секрет | Описание |
|--------|----------|
| `KEYSTORE_BASE64` | Keystore в base64: `base64 -w 0 keystore.jks` |
| `KEYSTORE_PASSWORD` | Пароль от keystore |
| `KEY_ALIAS` | Alias ключа |
| `KEY_PASSWORD` | Пароль от ключа |

### Создать keystore (если нет):
```bash
keytool -genkey -v -keystore openeljur.jks \
  -alias openeljur -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 openeljur.jks
```

## Запуск

- **При пуше тега** `v*` → собирает и создаёт Release:
  ```bash
  git tag v1.0.0 && git push --tags
  ```
- **Вручную** → Actions → Build & Release APK → Run workflow
