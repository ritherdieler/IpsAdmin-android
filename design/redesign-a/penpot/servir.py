#!/usr/bin/env python3
"""Serve design/redesign-a on http://localhost:8766 with CORS so Penpot can load the import plugin.

Usage: python3 penpot/servir.py   (from design/redesign-a), then in Penpot open the plugin manager
(Ctrl + Alt + P) and install http://localhost:8766/penpot/manifest.json. Also accepts a POST of
penpot/diseno.json from ui.html?modo=exportar (see consola.js).
"""
import functools
import http.server
from pathlib import Path

PORT = 8766
ROOT = Path(__file__).resolve().parent.parent


class CorsHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Private-Network", "true")
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_POST(self):
        # Only the export of ui.html?modo=exportar may write, and only this file.
        if self.path != "/penpot/diseno.json":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", "0"))
        (ROOT / "penpot" / "diseno.json").write_bytes(self.rfile.read(length))
        self.send_response(204)
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.end_headers()


if __name__ == "__main__":
    handler = functools.partial(CorsHandler, directory=str(ROOT))
    with http.server.ThreadingHTTPServer(("127.0.0.1", PORT), handler) as server:
        print(f"Sirviendo {ROOT} en http://localhost:{PORT}")
        server.serve_forever()
