#!/usr/bin/env python3
"""Export the canvas sources (fuente/*.dc.html) to standalone HTML pages (pantallas/).

Each page keeps the canvas markup, moves the <helmet> styles into <head>, drops the canvas
runtime and inlines images as data URIs, so a page opens in any browser and imports into Figma
(html.to.design, File tab) as a single file. Links between boards point to the exported files;
fuente/enlaces.json adds the missing ones and defines the happy paths, which navegacion.js
walks with ?flujo=<id>&paso=<n>. Also writes index.html (every board) and flujos.html.

Usage: python3 exportar.py   (exits with 1 when a rule or a flow step no longer matches)
"""
import base64
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "fuente"
ASSETS = SOURCE / "assets"
OUT = ROOT / "pantallas"

LINKS = SOURCE / "enlaces.json"
SCRIPT = ROOT / "navegacion.js"
MAP_SCRIPT = ROOT / "mapa.js"

BLOB = re.compile(r"/_blob/([0-9a-f]{32})")
SVG = re.compile(r"<svg\b.*?</svg>", re.S)
TAG = re.compile(r"<[^>]+>")
NAV = re.compile(r'<nav\b[^>]*aria-label="Navegación principal"[^>]*>.*?</nav>', re.S)
ARIA = re.compile(r'aria-label="([^"]*)"')
HREF = re.compile(r'href="([^"]*)"')
HELMET = re.compile(r"<helmet>(.*?)</helmet>", re.S)
BOARD = re.compile(r"<x-dc>(.*?)</x-dc>", re.S)


def inline_blob(match: re.Match) -> str:
    image = ASSETS / f"{match.group(1)}.png"
    return "data:image/png;base64," + base64.b64encode(image.read_bytes()).decode("ascii")


def label(attrs: str, body: str) -> str:
    """aria-label, else the visible text with tags read as spaces (same rule as navegacion.js)."""
    aria = ARIA.search(attrs)
    if aria:
        return html.unescape(aria.group(1)).strip()
    return re.sub(r"\s+", " ", html.unescape(TAG.sub(" ", SVG.sub(" ", body)))).strip()


def matches(text: str, key: str) -> bool:
    return text.startswith(key[:-1]) if key.endswith("*") else text == key


class Linker:
    """Points links at the exported files and applies fuente/enlaces.json to one board."""

    def __init__(self, links: dict, screens: set, problems: list):
        self.links, self.screens, self.problems = links, screens, problems

    def destination(self, screen: str, text: str, dead: bool, in_nav: bool, used: set):
        for key, target in self.links["pantallas"].get(screen, {}).items():
            if key != "*" and matches(text, key):
                used.add(key)
                return target
        if dead and in_nav:
            return self.links["barra_inferior"].get(text)
        return None

    def apply(self, screen: str, source: str) -> str:
        used: set = set()

        def nav_spans(text):
            return [m.span() for m in NAV.finditer(text)]

        def rewrite(kind):
            pattern = re.compile(rf"<{kind}\b([^>]*)>(.*?)</{kind}>", re.S)
            spans = nav_spans(current)

            def one(match):
                attrs, body = match.groups()
                href = HREF.search(attrs)
                dead = kind == "button" or (href is not None and href.group(1) == "#")
                in_nav = any(start <= match.start() < end for start, end in spans)
                target = self.destination(screen, label(attrs, body), dead, in_nav, used)
                if target:
                    self.check(screen, target)
                    if kind == "a" and href:
                        attrs = attrs.replace(href.group(0), f'href="{target}.html"')
                    else:
                        attrs += f' data-destino="{target}.html"'
                elif href:
                    attrs = attrs.replace(href.group(0), 'href="' + href.group(1).replace(".dc.html", ".html") + '"')
                return f"<{kind}{attrs}>{body}</{kind}>"

            return pattern.sub(one, current)

        current = source
        current = rewrite("button")
        current = rewrite("a")
        whole = self.links["pantallas"].get(screen, {}).get("*")
        if whole:
            self.check(screen, whole)
            used.add("*")
            current = current.replace("<body>", f'<body data-destino="{whole}.html">', 1)
        for key in self.links["pantallas"].get(screen, {}):
            if key not in used:
                self.problems.append(f"{screen}: ninguna etiqueta coincide con «{key}»")
        return current

    def check(self, screen: str, target: str):
        if target not in self.screens:
            self.problems.append(f"{screen}: el destino {target} no existe")


def labels(page: str) -> list:
    return [label(m.group(2), m.group(3)) for m in re.finditer(r"<(a|button)\b([^>]*)>(.*?)</\1>", page, re.S)]


def standalone(source: str, title: str) -> str:
    board = BOARD.search(source).group(1)
    helmet = HELMET.search(board)
    head = helmet.group(1).strip() if helmet else ""
    body = HELMET.sub("", board).strip()
    page = (
        "<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
        f"<title>{html.escape(title)}</title>\n{head}\n</head>\n<body>\n{body}\n"
        '<script src="flujos.js"></script>\n<script src="navegacion.js"></script>\n</body>\n</html>\n'
    )
    return BLOB.sub(inline_blob, page)


def index(canvas: dict, flow_count: int) -> str:
    boards = canvas["boards"]
    sections = []
    for page in canvas["pages"]:
        names = [name for name in canvas["order"] if boards[name]["page"] == page["id"]]
        cards = []
        for name in names:
            board = boards[name]
            target = name.replace(".dc.html", ".html")
            scale = 280 / board["w"] if board["w"] > 400 else 0.5
            width, height = round(board["w"] * scale), round(board["h"] * scale)
            cards.append(
                f'<a class="card" href="{target}" style="width:{width}px">'
                f'<div class="frame" style="width:{width}px;height:{height}px">'
                f'<iframe src="{target}" loading="lazy" tabindex="-1" scrolling="no" '
                f'style="width:{board["w"]}px;height:{board["h"]}px;transform:scale({scale:.4f})"></iframe></div>'
                f'<span>{html.escape(board["title"])}</span><code>{target}</code></a>'
            )
        sections.append(f'<section><h2>{html.escape(page["name"])}</h2><div class="grid">{"".join(cards)}</div></section>')
    return (
        "<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
        f"<title>{html.escape(canvas['title'])}</title>\n<style>\n"
        "body{margin:0;padding:24px 16px 48px;background:#eef0f5;color:#10142b;font-family:system-ui,sans-serif}\n"
        "h1{margin:0 0 4px;font-size:24px}p{margin:0 0 24px;color:#4a5068}\n"
        "h2{margin:32px 0 12px;font-size:18px}.grid{display:flex;flex-wrap:wrap;gap:20px}\n"
        ".card{display:flex;flex-direction:column;gap:6px;color:inherit;text-decoration:none}\n"
        ".card span{font-size:14px;font-weight:600}.card code{font-size:11px;color:#4a5068}\n"
        ".frame{overflow:hidden;border-radius:12px;background:#fff;box-shadow:0 1px 3px rgba(16,20,43,.15)}\n"
        "iframe{border:0;transform-origin:0 0;pointer-events:none;display:block}\n"
        "</style>\n</head>\n<body>\n"
        f"<h1>{html.escape(canvas['title'])}</h1>\n"
        f"<p>{len(boards)} tableros exportados del lienzo. Toca uno para abrirlo a tamaño real y navega tocando sus botones. "
        f'<a href="flujos.html">Ver los {flow_count} happy paths →</a> · <a href="mapa.html">Mapa de navegación →</a></p>\n'
        + "\n".join(sections)
        + "\n</body>\n</html>\n"
    )


def flows_page(canvas: dict, flows: list) -> str:
    boards = canvas["boards"]
    scale = 0.32
    blocks = []
    for flow in flows:
        steps = []
        for number, step in enumerate(flow["pasos"]):
            board = boards[step["pantalla"] + ".dc.html"]
            url = f'{step["pantalla"]}.html?flujo={flow["id"]}&amp;paso={number}'
            action = "Toca la pantalla" if step.get("toca") == "*" else (
                f'Toca «{html.escape(step["toca"].rstrip("*"))}{"…" if step["toca"].endswith("*") else ""}»' if step.get("toca") else "")
            note = html.escape(step.get("nota", ""))
            width, height = round(board["w"] * scale), round(min(board["h"], 844) * scale)
            steps.append(
                f'<a class="paso" href="{url}" style="width:{width}px"><div class="frame" style="width:{width}px;height:{height}px">'
                f'<iframe src="{step["pantalla"]}.html" loading="lazy" tabindex="-1" scrolling="no" '
                f'style="width:{board["w"]}px;height:{board["h"]}px;transform:scale({scale})"></iframe></div>'
                f'<span class="n">{number + 1}. {html.escape(board["title"])}</span>'
                + (f'<span class="toca">{action}</span>' if action else "")
                + (f'<span class="nota">{note}</span>' if note else "")
                + "</a>"
            )
        badge = '<span class="clave">Flujo clave del diseño</span>' if flow.get("clave") else ""
        blocks.append(
            f'<section id="{flow["id"]}"><div class="cab"><h2>{html.escape(flow["nombre"])}</h2>{badge}'
            f'<span class="meta">{html.escape(flow.get("rol", ""))} · {len(flow["pasos"])} pantallas</span>'
            f'<a class="ir" href="{flow["pasos"][0]["pantalla"]}.html?flujo={flow["id"]}&amp;paso=0">Recorrer →</a>'
            f'<a class="ir" href="mapa.html#flujo={flow["id"]}">Ver diagrama →</a></div>'
            f'<div class="pasos">{"<span class=flecha>→</span>".join(steps)}</div></section>'
        )
    key_flows = sum(1 for flow in flows if flow.get("clave"))
    return (
        "<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
        "<title>Happy paths</title>\n<style>\n"
        "body{margin:0;padding:24px 16px 48px;background:#eef0f5;color:#10142b;font-family:system-ui,sans-serif}\n"
        "h1{margin:0 0 4px;font-size:24px}p{margin:0 0 8px;color:#4a5068;max-width:900px}\n"
        "section{margin-top:28px}.cab{display:flex;flex-wrap:wrap;align-items:baseline;gap:8px 12px;margin-bottom:10px}\n"
        "h2{margin:0;font-size:18px}.meta{color:#4a5068;font-size:13px}\n"
        ".clave{font-size:12px;font-weight:600;padding:2px 8px;border-radius:6px;background:#ffe2cc;color:#8a3a00}\n"
        ".ir{font-weight:600;color:#2f4ad0;text-decoration:none}\n"
        ".pasos{display:flex;align-items:flex-start;gap:8px;overflow-x:auto;padding-bottom:8px}\n"
        ".flecha{align-self:center;color:#8a90a8;font-size:20px;margin-top:-40px}\n"
        ".paso{flex:none;display:flex;flex-direction:column;gap:4px;color:inherit;text-decoration:none}\n"
        ".paso .n{font-size:13px;font-weight:600}.paso .toca{font-size:12px;color:#c25100}.paso .nota{font-size:12px;color:#4a5068}\n"
        ".frame{overflow:hidden;border-radius:10px;background:#fff;box-shadow:0 1px 3px rgba(16,20,43,.15)}\n"
        "iframe{border:0;transform-origin:0 0;pointer-events:none;display:block}\n"
        "</style>\n</head>\n<body>\n"
        f"<h1>Happy paths · {len(flows)}</h1>\n"
        f"<p>{key_flows} son los flujos clave del mapa del diseño (A · Mapa de la app); el resto cubre cada tarea principal. "
        "«Recorrer» abre el primer paso con un panel que resalta qué tocar; también se avanza con las flechas del teclado. "
        '<a href="index.html">Ver todas las pantallas</a> · <a href="mapa.html">Mapa de navegación</a></p>\n'
        + "\n".join(blocks)
        + "\n</body>\n</html>\n"
    )


def map_page(canvas: dict) -> str:
    data = {
        "pages": canvas["pages"],
        "order": [name.replace(".dc.html", "") for name in canvas["order"]],
        "boards": {name.replace(".dc.html", ""): board for name, board in canvas["boards"].items()},
    }
    return (
        "<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
        "<title>Mapa de navegación</title>\n<style>\n"
        "body{margin:0;background:#eceef3;color:#10142b;font-family:system-ui,sans-serif}\n"
        "header{position:sticky;top:0;left:0;z-index:10;display:flex;flex-wrap:wrap;align-items:center;gap:8px 14px;"
        "padding:10px 16px;background:#fff;box-shadow:0 1px 3px rgba(16,20,43,.15);font-size:14px}\n"
        "header b{font-size:16px}header select,header button{font:inherit;padding:4px 8px}header a{color:#2f4ad0}\n"
        "#stage{position:relative;margin:24px 16px}#inner{position:absolute;left:0;top:0;transform-origin:0 0}\n"
        ".frame{position:absolute;background:#fff;box-shadow:0 2px 8px rgba(16,20,43,.18);border-radius:4px}\n"
        ".frame iframe{border:0;display:block;pointer-events:none}\n"
        ".titulo{position:absolute;left:0;top:-44px;font-size:24px;font-weight:600;white-space:nowrap;color:#10142b}\n"
        ".nota{position:absolute;left:0;bottom:-40px;font-size:20px;color:#4a5068;white-space:nowrap}\n"
        "#lines{position:absolute;left:0;top:0;overflow:visible;pointer-events:none;z-index:2}\n"
        "path.enlace{fill:none;stroke:#0d99ff;stroke-width:2.5;vector-effect:non-scaling-stroke;marker-end:url(#flecha)}\n"
        "path.auto{stroke-dasharray:8 6}circle.origen{fill:#0d99ff}\n"
        ".toque{position:absolute;z-index:1;border:3px solid #0d99ff;border-radius:8px;background:rgba(13,153,255,.08);pointer-events:none}\n"
        ".fuera{position:absolute;z-index:3;font:600 18px system-ui,sans-serif;background:#fff;border:2px solid #0d99ff;color:#0b6fc2;"
        "border-radius:999px;padding:6px 14px;white-space:nowrap;cursor:pointer}\n"
        "#inner.enfocado [data-from]:not(.activo){opacity:.1}\n"
        "</style>\n</head>\n<body>\n<header><b>Mapa de navegación</b>"
        "<label>Módulo <select id=\"modulo\"></select></label>"
        "<label>Happy path <select id=\"flujo\"></select></label>"
        "<label><input type=\"checkbox\" id=\"barra\"> Barra inferior</label>"
        "<label><input type=\"checkbox\" id=\"volver\"> Atrás y cerrar</label>"
        "<span><button id=\"menos\">−</button> <span id=\"zoom\"></span> <button id=\"mas\">+</button></span>"
        "<a href=\"flujos.html\">Happy paths</a><a href=\"index.html\">Pantallas</a></header>\n"
        "<div id=\"stage\"><div id=\"inner\"><svg id=\"lines\"></svg></div></div>\n"
        f"<script>window.MAPA = {json.dumps(data, ensure_ascii=False)};</script>\n"
        "<script src=\"flujos.js\"></script>\n<script src=\"mapa.js\"></script>\n</body>\n</html>\n"
    )


def check_flows(flows: list, pages: dict, problems: list) -> None:
    for flow in flows:
        for number, step in enumerate(flow["pasos"]):
            page = pages.get(step["pantalla"])
            if page is None:
                problems.append(f"flujo {flow['id']} paso {number + 1}: no existe {step['pantalla']}")
                continue
            key = step.get("toca")
            if key and key != "*" and not any(matches(text, key) for text in labels(page)):
                problems.append(f"flujo {flow['id']} paso {number + 1}: {step['pantalla']} no tiene «{key}»")


def main() -> None:
    canvas = json.loads((SOURCE / "canvas.json").read_text(encoding="utf-8"))
    links = json.loads(LINKS.read_text(encoding="utf-8"))
    flows = links["flujos"]
    screens = {name.replace(".dc.html", "") for name in canvas["boards"]}
    problems: list = []
    linker = Linker(links, screens, problems)
    OUT.mkdir(exist_ok=True)
    for stale in OUT.glob("*.html"):
        stale.unlink()
    pages = {}
    for name, board in canvas["boards"].items():
        screen = name.replace(".dc.html", "")
        page = linker.apply(screen, standalone((SOURCE / name).read_text(encoding="utf-8"), board["title"]))
        pages[screen] = page
        (OUT / f"{screen}.html").write_text(page, encoding="utf-8")
    check_flows(flows, pages, problems)
    (OUT / "flujos.js").write_text("window.FLUJOS = " + json.dumps(flows, ensure_ascii=False) + ";\n", encoding="utf-8")
    (OUT / "navegacion.js").write_text(SCRIPT.read_text(encoding="utf-8"), encoding="utf-8")
    (OUT / "index.html").write_text(index(canvas, len(flows)), encoding="utf-8")
    (OUT / "flujos.html").write_text(flows_page(canvas, flows), encoding="utf-8")
    (OUT / "mapa.js").write_text(MAP_SCRIPT.read_text(encoding="utf-8"), encoding="utf-8")
    (OUT / "mapa.html").write_text(map_page(canvas), encoding="utf-8")
    linked = sum(len(re.findall(r'(?:href|data-destino)="a-[^"#]+\.html"', page)) for page in pages.values())
    print(f"{len(pages)} pantallas, {linked} enlaces entre pantallas y {len(flows)} happy paths en {OUT}")
    for problem in problems:
        print("AVISO:", problem)
    if problems:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
