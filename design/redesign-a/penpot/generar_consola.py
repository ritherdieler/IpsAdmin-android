#!/usr/bin/env python3
"""Rebuild consola.js from plugin.js: the same import code, run from the Penpot tab's console."""
from pathlib import Path

HERE = Path(__file__).resolve().parent
head = (HERE / "_cabecera_consola.js").read_text(encoding="utf-8")
body = (HERE / "plugin.js").read_text(encoding="utf-8")
indented = "\n".join("  " + line if line.strip() else line for line in body.splitlines())
(HERE / "consola.js").write_text(head + indented + "\n})();\n", encoding="utf-8")
print("consola.js regenerado")
