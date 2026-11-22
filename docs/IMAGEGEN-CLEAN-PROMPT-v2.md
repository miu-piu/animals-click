# Зверята 2.0 — единый шаблон для генератора

Для каждого вызова подставь только одно животное из `catalog/catalog-v2.json`: поле `reference` задаёт вид/породу, `stage` — возрастную форму. Никаких индивидуальных указаний по окраске, позе, ракурсу и месту съёмки.

```text
Create one photorealistic vertical 9:16 photograph of a single [ANIMAL REFERENCE], [LIFE STAGE].
Show the entire animal with natural proportions and accurate anatomy, immediately recognizable by a young child.
Choose a realistic species-appropriate setting and a natural pose with a calm, simple background and soft daylight.
One uninterrupted photographic scene with the animal as the clear main subject.
```

Заменить `[ANIMAL REFERENCE]` и `[LIFE STAGE]` фактическими значениями текущей записи. Вызов генератора содержит только заполненный текст из блока. Оценка результата, журнал и производственные десятки находятся вне генерационного запроса.
