---
name: build-desktop-release
description: Проверенная локальная сборка payment-courier-tool для Windows. Использовать при выпуске shaded JAR, app-image EXE и ZIP, изменениях параметров памяти/параллельности, проверке лимита страниц и подготовке артефакта для передачи пользователю.
---

# Сборка desktop-релиза

## Выпускать только после проверки

1. Запустить весь `mvn test`.
2. Собрать shaded JAR командой `mvn -DskipTests package`.
3. Убедиться, что `target/payment-courier-tool.jar` обновлён.
4. Собрать `jpackage --type app-image`, а не installer.
5. Упаковать всю директорию приложения в ZIP: один EXE без `runtime` и `app` не переносим.
6. Проверить `PaymentCourierTool.cfg` и содержимое ZIP.

Для стандартной сборки запускать `scripts/build-release.ps1` из корня проекта или по полному пути.

## Сохранять параметры рабочего режима

- Java 17.
- `-Xms512m`.
- `-XX:MaxRAMPercentage=80.0`.
- `-DpaymentCourier.fast.pdfThreads=12`.
- `-DpaymentCourier.fast.writerThreads=2`.
- Не менять лимит 5000 страниц на выходной PDF без отдельного требования и теста.

## Не смешивать артефакты

- Использовать чистые `target/package-input-<tag>` и `target/exe-<tag>`.
- Не включать старые JAR в `--input`.
- В ответе указывать абсолютные пути JAR, EXE и ZIP.
- Не создавать installer, пока это отдельно не запрошено.
- Не выполнять Git push или commit в рамках сборки.

## Поддерживать skill

Обновлять skill и script при изменении main-класса, версии Java, JVM-параметров, формата артефакта или обязательных release-проверок.