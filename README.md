# Зверята 2.0 — Android (рабочая ветка)

**На телефоне:** «Зверята» · **package:** `com.miupiu.zveryata` · **preview version:** 2.0.0-preview.1.

Новая реализация Kotlin/Jetpack Compose в **том же репозитории**. Старая версия сохранена в `archive/zveryata-v1-before-v2`.

## Генерация изображений

Журнал прогресса: `catalog/image-production-state-v2.json`. Карточка №001 `ari` отмечена как `skipped`. **Первая генерация — №002 `cat`**.

[**Стартовый мастер-промпт — только новая генерация исходников с нуля**](docs/MASTER-PROMPT-GENERATION-ONLY-v2.md). Пока работает эта стадия, не собирать промежуточные обзоры, ZIP и APK; WebP и финальная визуальная приёмка — отдельный последующий этап.

## Зафиксированные ТЗ и продолжение работы

1. [**Основное ТЗ приложения**](docs/TZ-v2.md) — экран, навигация, Android, тесты, подпись и условия релиза.
2. [**Отдельное ТЗ изображений**](docs/TZ-images-v2.md) — конкретный визуальный стандарт, генерация строго одной карточки за вызов, брак, обработка и приёмка.
   - [**Мастер-промпт на всё производство без подтверждений между партиями**](docs/MASTER-PROMPT-IMAGE-PRODUCTION-v2.md) — последовательное автономное выполнение 15 партий, сохранение прогресса и восстановление после ограничений инструментов.
   - [**Чистый позитивный шаблон для ImageGen**](docs/IMAGEGEN-CLEAN-PROMPT-v2.md) — только один конкретный вид и одна фотографическая сцена, без описания партий и истории ошибок.
3. [**Каталог 142 карточек**](catalog/catalog-v2.json) — единственный точный список с ID, названием, видом, возрастом и сценой.
4. [**15 партий по 10 / 2 карточки**](catalog/batches-v2.json) — производственный порядок, производный от каталога.
5. `scripts/make_generation_briefs_v2.py` — воспроизводимые индивидуальные задания для 141 новой картинки (Ари исключена).

**В новом чате:** открыть четыре файла выше, найти следующую незавершённую карточку, сгенерировать **одно животное одним вызовом**, проверить, сохранить оригинал и WebP. Коллажи создаёт только программный сборщик обзора после генерации; прежние картинки не переиспользовать, Ари не генерировать. Не путать 59 legacy-фотографий в preview APK с новой коллекцией.


## Статус

Это **первый исполняемый прототип**, а не выпуск 142 новых фотографий. В нём старые 59 WebP используются только для проверки нового экрана, навигации и таймера. Окончательные 142 карточки и описания сцен лежат в `catalog/catalog-v2.json`. Старые фото будут заменены после индивидуальной проверки новой серии.

## Поведение прототипа

- Отдельная адаптивная чёрная шапка с центрированным названием и фото до боковых и нижнего краёв.
- Влево/вверх — вперёд; вправо/вниз — назад; короткий тап не перелистывает.
- Порог свайпа 56 dp, уверенное преимущество основной оси; независимое слежение за несколькими пальцами.
- Глобальный 3-секундный cooldown только для принятых ручных жестов.
- Автоматический переход раз в 30 секунд, пауза в фоне.
- Встроенные изображения без интернет-доступа во время работы, ограниченный байтами кеш.

## Сборка

GitHub Actions `.github/workflows/build-apk.yml` пока использует **legacy preview** скрипт синхронизации старых иллюстраций из `miu-piu/kids-games`. Это временный источник тестовых изображений, не архитектура финального выпуска. До выпуска 2.0 изображения должны находиться в собственном репозитории и проверяться по манифесту SHA-256.

Тесты/линт: `gradle :app:testDebugUnitTest :app:lintDebug`. Сборка: `gradle :app:assembleDebug`.

APK с CI debug-подписью не гарантирует обновление поверх ранее установленного debug APK из другой сборочной среды. Перед финальным выпуском нужен стабильный закрытый ключ подписи и проверка установки. 

## Single source of truth and image production

- `catalog/catalog-v2.json`: all 142 approved identities, order, labels, species, life stages and individual habitat notes.
- `scripts/make_generation_briefs_v2.py`: generate 142 individual image briefs. **141 new images; Ari is preserved, not generated.**
- `app/src/main/assets/catalog-preview.json`: temporary 59-image comparison set only.
- `scripts/package_content_v2.py --mode preview`: stage the preview runtime catalog and SHA-256 manifest after the legacy-preview sync.
- `scripts/package_content_v2.py --mode release`: fail unless **all 142** individually approved, correctly sized 1440x2560 WebP images are present in `assets-v2/approved/webp/`.

Release verification also requires `assets-v2/approved/sha256.json` with one record per file:

```json
{
  "amur-tiger.webp": {
    "status": "approved",
    "sha256": "64-lowercase-hex-characters-of-approved-WebP",
    "original_sha256": "64-lowercase-hex-characters-of-original-source",
    "review_notes": "Document actual independent anatomical and visual acceptance"
  }
}
```

Hashes above are illustrative placeholders, not valid release values. Source originals and their hashes must remain available for audit. **Creating hashes does not constitute visual approval.** Do not mark a card approved until the image has been inspected for species, age, correct limb count, clear silhouette, environment, scale, text artifacts and full-frame composition.

The legacy preview synchronizer is temporarily pinned to exact `miu-piu/kids-games` commit `176e1393e171510849e8cb7d11b22c022e8dd473`; production does not fetch images at build time.

## Tests

- `python3 -m unittest discover -s tests -p 'test_*v2.py' -v`: catalog uniqueness, protection against release with unreviewed/invalid assets, and special handling of Ari.
- `gradle :app:testDebugUnitTest`: direction, axis strength, boundary and multitouch-state rules.
- `gradle :app:lintDebug`, `gradle :app:assembleDebugAndroidTest`, `gradle :app:assembleDebug`: static analysis and test/binary compilation.
- `.github/workflows/android-ui-v2.yml`: actual Compose UI tests on Android emulator; captures FHD+/QHD+ emulator screenshots after test runs.

Passing tests in a CI emulator is **not** a substitute for validating real multi-finger usage, Samsung Screen Pinning and installation/signature behavior on the target phone.

## Подпись финальной версии

`.github/workflows/release-v2.yml` — отдельная ручная сборка только после приёмки **всех 142** изображений. Она использует четыре закрытых параметра GitHub Actions: `ZVERYATA_KEYSTORE_B64`, `ZVERYATA_KEYSTORE_PASSWORD`, `ZVERYATA_KEY_ALIAS`, `ZVERYATA_KEY_PASSWORD`. Без них процесс останавливается и не публикует неподписанный APK. Приватный ключ не добавляется в репозиторий.

Совпадение Android package ID `com.miupiu.zveryata` само по себе не позволяет заменить старый APK: нужен **тот же сертификат подписи**, которым была подписана установленная версия. Если прежняя версия подписана другим debug-ключом, требуется переустановка; обойти проверку Android нельзя.

## Файлы источников

`assets-v2/originals/legacy-ari.webp` — побайтно сохранённая историческая карточка Ари из старого исходного набора, SHA-256 `05d8c4828dc9a5c1ff9f8ccd412701e0629cd38bd804d6b80dff3134fc61675e`. Это **сохранённый файл прежней версии**, не доказательство происхождения фотографии. Изображение Ари не генерируется нейросетью в 2.0. Окончательный исходник должен пройти визуальную приёмку на соответствие конкретной собаке.

Иконка `app/src/main/res/drawable-nodpi/zveryata_icon.png` теперь хранится в репозитории; финальная сборка не скачивает её из старой PWA.
