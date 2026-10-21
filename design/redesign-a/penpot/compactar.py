#!/usr/bin/env python3
"""Compact penpot/diseno.json for the Penpot MCP import (penpot/mcp.js decodes it).

Shared tables (styles, colors, icons, images) go once; each board is a list of short arrays:
  rect  ["r", x, y, w, h, fill, stroke, radius, shadow]
  text  ["t", x, y, w, h, text, style, flags, segments]   flags: 1 wrap, 2 fixed width
  svg   ["s", x, y, w, h, icon]
  image ["i", x, y, w, h, image, radius]
Output: penpot/mcp/tablas.json and penpot/mcp/<n>-<screen>.json (one per step).
"""
import base64
import hashlib
import re
import json
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE / "mcp"


def num(v):
    r = round(v, 1)
    return int(r) if r == int(r) else r


class Table:
    def __init__(self):
        self.items, self.index = [], {}

    def key(self, value):
        if value is None:
            return None
        k = json.dumps(value, sort_keys=True, ensure_ascii=False)
        if k not in self.index:
            self.index[k] = len(self.items)
            self.items.append(value)
        return self.index[k]


styles, colors, icons, images, shadows, radii = Table(), Table(), Table(), Table(), Table(), Table()


def color(c):
    return colors.key([c["color"], num(c["opacity"])]) if c else None


def style(n):
    return styles.key([n["family"], str(n["weight"]), bool(n["italic"]), num(n["size"]), num(n["lineHeight"]),
                       num(n["letterSpacing"]), color({"color": n["color"], "opacity": n["opacity"]}),
                       n.get("transform"), n.get("decoration"), n.get("align", "left")])


SHRUNK = {}


def shrink(raw, w, h):
    """Resize an image to 1.5x its largest use (macOS sips): opaque screenshots become JPEG, the logo stays PNG."""
    digest = hashlib.sha1(raw).hexdigest()
    target = max(SHRUNK.get(digest, (0, None))[0], round(max(w, h) * (1 if h > w * 1.5 else 1.5)))
    with tempfile.TemporaryDirectory() as tmp:
        src = Path(tmp) / "in.png"
        src.write_bytes(raw)
        alpha = "yes" in subprocess.run(["sips", "-g", "hasAlpha", str(src)], capture_output=True, text=True).stdout
        opaque = h > w * 1.5  # app screenshots: tall and without transparency where it matters
        out = Path(tmp) / ("out.jpg" if opaque else "out.png")
        args = ["sips", "-Z", str(target)] + (["-s", "format", "jpeg", "-s", "formatOptions", "60"] if opaque else []) + [str(src), "--out", str(out)]
        subprocess.run(args, capture_output=True, check=True)
        result = ("image/jpeg" if opaque else "image/png", out.read_bytes())
    SHRUNK[digest] = (target, result)
    return result


ROOT_ICON = re.compile(
    r'^<svg width="([\d.]+)" height="([\d.]+)" viewBox="([^"]+)" fill="none" stroke="(#[0-9a-f]{6})" stroke-width="([\d.]+)" '
    r'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">(.*)</svg>$', re.S)


def icon(markup):
    """Line icons travel as [w, h, viewBox, color, strokeWidth, children without the inherited
    fill/stroke/stroke-width]; mcp.js puts them back. Anything else stays as raw SVG."""
    m = ROOT_ICON.match(markup)
    if not m:
        return markup
    w, h, box, color, width, inner = m.groups()
    inherited = f' fill="none" stroke="{color}" stroke-width="{width}"'
    inner = inner.replace(inherited, "")
    inner = re.sub(r"></(path|circle|rect|line|polyline|polygon|ellipse)>", "/>", inner)
    return [w, h, box, color, width, inner]


def compact_node(n):
    if n["type"] == "rect":
        stroke = [color(n["stroke"]), num(n["stroke"]["width"])] if n.get("stroke") else None
        rad = n.get("radius")
        rad = radii.key([num(v) for v in rad]) if rad and any(rad) else None
        sh = shadows.key([[int(s["inset"]), num(s["x"]), num(s["y"]), num(s["blur"]), num(s["spread"]), s["color"], num(s["opacity"])] for s in n["shadows"]]) if n.get("shadows") else None
        return ["r", num(n["x"]), num(n["y"]), num(n["w"]), num(n["h"]), color(n.get("fill")), stroke, rad, sh]
    if n["type"] == "text":
        flags = (1 if n.get("wrap") else 0) | (2 if n.get("fixedWidth") else 0)
        segs = [[s["start"], s["end"], style(s)] for s in n["segments"]] if n.get("segments") else None
        return ["t", num(n["x"]), num(n["y"]), num(n["w"]), num(n["h"]), n["text"], style(n), flags, segs]
    if n["type"] == "svg":
        return ["s", num(n["x"]), num(n["y"]), num(n["w"]), num(n["h"]), icons.key(icon(n["svg"]))]
    if n["type"] == "image":
        mime, data = shrink(bytes(n["bytes"]), n["w"], n["h"])
        data = base64.b64encode(data).decode()
        rad = n.get("radius")
        return ["i", num(n["x"]), num(n["y"]), num(n["w"]), num(n["h"]), images.key([mime, data]), rad if rad and any(rad) else None]
    raise ValueError(n["type"])


def main():
    steps = json.loads((HERE / "diseno.json").read_text(encoding="utf-8"))
    OUT.mkdir(exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()
    out = []
    for step in steps:
        if step["type"] == "board":
            out.append({
                "type": "board", "screen": step["screen"], "title": step["title"],
                "box": [num(step["x"]), num(step["y"]), num(step["w"]), num(step["h"])],
                "fill": color(step["fill"]),
                "nodes": [compact_node(n) for n in step["nodes"]],
                "links": [[num(h["x"]), num(h["y"]), num(h["w"]), num(h["h"]), h["target"]] for h in step["hotspots"]],
            })
        else:
            out.append(step)
    tables = {"styles": styles.items, "colors": colors.items, "icons": icons.items, "images": images.items,
              "shadows": shadows.items, "radii": radii.items}
    (OUT / "tablas.json").write_text(json.dumps(tables, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    for i, step in enumerate(out):
        name = f"{i:03d}-{step.get('screen', step['type'])}.json"
        (OUT / name).write_text(json.dumps(step, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    sizes = sorted(((p.stat().st_size, p.name) for p in OUT.glob("*.json")), reverse=True)
    total = sum(s for s, _ in sizes)
    print(f"{len(out)} pasos, total {total // 1024} KB; tablas {(OUT / 'tablas.json').stat().st_size // 1024} KB "
          f"(iconos {len(icons.items)}, estilos {len(styles.items)}, colores {len(colors.items)}, imágenes {len(images.items)})")
    print("más grandes:", ", ".join(f"{n} {s // 1024}KB" for s, n in sizes[:5]))


if __name__ == "__main__":
    main()
