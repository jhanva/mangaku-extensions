#!/usr/bin/env python3
"""Genera el repo de extensiones (index.min.json + APKs) a partir de extensions/*.

Cada modulo de extension declara sus metadatos en extension.json. Este script:
1. Localiza el APK compilado del modulo (variant debug o release).
2. Lo copia a <salida>/apk/<pkg>.apk.
3. Escribe <salida>/index.min.json e index.json con el formato que consume la app
   (el mismo esquema que publica Keiyoushi).

Uso:
  python scripts/generate_extensions_index.py [--variant debug|release] [--output build/extensions-repo]

Requiere haber compilado antes los modulos, por ejemplo:
  gradlew :extensions:weebcentral:assembleDebug :extensions:senmanga:assembleDebug
"""

import argparse
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXTENSIONS_DIR = ROOT / "extensions"


def find_apk(module_dir: Path, variant: str) -> Path | None:
    apk_dir = module_dir / "build" / "outputs" / "apk" / variant
    if not apk_dir.is_dir():
        return None
    apks = sorted(apk_dir.glob("*.apk"))
    return apks[0] if apks else None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--variant", choices=["debug", "release"], default="debug")
    parser.add_argument("--output", default=str(ROOT / "build" / "extensions-repo"))
    args = parser.parse_args()

    output = Path(args.output)
    apk_output = output / "apk"
    apk_output.mkdir(parents=True, exist_ok=True)

    entries = []
    missing = []
    for module_dir in sorted(EXTENSIONS_DIR.iterdir()):
        meta_file = module_dir / "extension.json"
        if not meta_file.is_file():
            continue
        meta = json.loads(meta_file.read_text(encoding="utf-8"))
        apk = find_apk(module_dir, args.variant)
        if apk is None:
            missing.append(module_dir.name)
            continue
        apk_name = f"{meta['pkg']}.apk"
        shutil.copy2(apk, apk_output / apk_name)
        entries.append(
            {
                "name": f"Mangaku: {meta['name']}",
                "pkg": meta["pkg"],
                "apk": apk_name,
                "lang": meta["lang"],
                "code": meta["code"],
                "version": meta["version"],
                "nsfw": meta.get("nsfw", 0),
                "sources": meta.get("sources", []),
            }
        )

    if missing:
        print(f"ERROR: faltan APKs ({args.variant}) para: {', '.join(missing)}", file=sys.stderr)
        print("Compila primero: gradlew " + " ".join(f":extensions:{m}:assemble{args.variant.capitalize()}" for m in missing), file=sys.stderr)
        return 1

    (output / "index.min.json").write_text(
        json.dumps(entries, separators=(",", ":"), ensure_ascii=False), encoding="utf-8"
    )
    (output / "index.json").write_text(
        json.dumps(entries, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(f"OK: {len(entries)} extension(es) en {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
