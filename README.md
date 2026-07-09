# Mangaku Extensions

Catalogo de extensiones de Mangaku Reader: indice (`index.min.json`) + APKs firmados.

Este repositorio contiene solo artefactos publicados; el codigo fuente de las extensiones vive en el
repositorio principal (privado) y se publica aqui via CI o manualmente.

La app consume este catalogo desde:

```
https://raw.githubusercontent.com/jhanva/mangaku-extensions/main
```

- `index.min.json`: indice que lista las extensiones disponibles (nombre, paquete, version, fuentes).
- `apk/`: un APK por extension, firmado con el keystore de confianza registrado en la app.
