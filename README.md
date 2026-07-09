# Mangaku Extensions

Codigo fuente **y** catalogo publicado de las extensiones de Mangaku Reader. La app lector
(`mangaku-reader`) solo trae MangaDex integrado; todo el resto del catalogo son extensiones que viven
y se construyen aqui.

La app consume el catalogo desde:

```
https://raw.githubusercontent.com/jhanva/mangaku-extensions/main
```

## Estructura

- `core/tachiyomi-api/`: copia del **contrato** de la API `eu.kanade.tachiyomi.*` (+ shim Injekt) que
  la app anfitriona implementa y provee en runtime. Las extensiones y plantillas compilan contra este
  modulo con `compileOnly` (no se bundlea). Debe mantenerse sincronizado con la copia del lector.
- `lib-multisrc/`: plantillas multi-sitio (`madara`, `mangabox`). Cada una se **bundlea** dentro de
  los APKs que la usan (`implementation`).
- `extensions/`: una carpeta por extension (fuente + `extension.json` + tests). Cada una compila a un
  APK diminuto que solo aporta su codigo de fuente.
- `scripts/generate_extensions_index.py`: genera `index.min.json` + `apk/` a partir de los APKs.
- `apk/` e `index*.json` (raiz): el **catalogo publicado**, regenerado por el CI en cada cambio de
  codigo. No se editan a mano.

## Como anadir una extension

1. Fuente directa (API/JSON o scraping simple): crea `extensions/<nombre>/` extendiendo
   `HttpSource` (mira `senmanga` o `comick`).
2. Sitio de una familia con plantilla (WordPress Madara, MangaBox...): extiende la plantilla de
   `lib-multisrc/` y anade `implementation(project(":lib-multisrc:<tema>"))` (mira `mangaread` o
   `manganato`).
3. Declara metadatos en `extension.json`, registra el modulo en `settings.gradle.kts` y anadelo al
   `assembleRelease` del workflow `publish`.

## Build local

```
./gradlew :extensions:manganato:assembleDebug        # un APK de extension
./gradlew testDebugUnitTest                           # todos los tests
python scripts/generate_extensions_index.py --variant debug --output build/catalog
```

Los APKs de release se firman con el keystore de confianza (SHA-256 registrado en la app); el CI lo
inyecta desde secrets. Sin keystore, el release queda sin firmar y la app en release lo rechaza.
