# KPDF Deployment Guide

This repository already contains publishing infrastructure for `kpdf-core` and `kpdf-compose`.

## Before Publishing

Update the publishing metadata in:

- [gradle.properties](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/gradle.properties:1)

Review:

- artifact coordinates
- POM metadata
- repository credentials
- signing configuration

## Validate Before Release

Run:

```bash
./gradlew \
  :kpdf-core:compileCommonMainKotlinMetadata \
  :kpdf-core:compileAndroidMain \
  :kpdf-compose:compileCommonMainKotlinMetadata \
  :kpdf-compose:compileAndroidMain \
  :kpdf-compose:compileKotlinIosSimulatorArm64 \
  :composeApp:compileDebugSources
```

## Recommendation

Publish only after the public API and the examples in [SDK.md](/Users/mahmoudkamal/AndroidStudioProjects/KPDF/docs/SDK.md:1) reflect the version you want to ship.
