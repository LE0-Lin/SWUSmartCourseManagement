"""Serve the actual advisor component with JSON from isolated JDBC tests."""
import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import mimetypes
from pathlib import Path
import subprocess
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
DEPS = ROOT / "SmartCourse-App/node_modules"
FILES = {
    "/": ROOT / "build/advisor-preview/index.html",
    "/assets/vue.js": DEPS / "vue/dist/vue.js",
    "/assets/element.js": DEPS / "element-ui/lib/index.js",
    "/assets/element.css": DEPS / "element-ui/lib/theme-chalk/index.css",
    "/assets/fonts/element-icons.woff": DEPS / "element-ui/lib/theme-chalk/fonts/element-icons.woff",
    "/assets/fonts/element-icons.ttf": DEPS / "element-ui/lib/theme-chalk/fonts/element-icons.ttf",
}
for scenario in ("full-catalog", "missing-candidate", "missing-selected", "search-limit"):
    FILES["/fixture/" + scenario] = ROOT / ("SmartCourse-Server/target/demo/advisor-" + scenario + ".json")


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = FILES.get(urlsplit(self.path).path)
        if path is None or not path.is_file():
            self.send_error(404, "Unknown route or missing fixture; run mvn test first")
            return
        content = path.read_bytes()
        self.send_response(200)
        mime = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        self.send_header("Content-Type", mime + ("; charset=utf-8" if mime.startswith("text/") or mime == "application/json" else ""))
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(content)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=19531)
    args = parser.parse_args()
    subprocess.run(["node", str(ROOT / "scripts/build_advisor_preview.cjs")], cwd=ROOT, check=True)
    missing = [str(path.relative_to(ROOT)) for path in FILES.values() if not path.is_file()]
    if missing:
        raise SystemExit("Run backend mvn test and student npm ci first. Missing: " + ", ".join(missing))
    print(f"Read-only fixture preview: http://127.0.0.1:{args.port}/", flush=True)
    with ThreadingHTTPServer(("127.0.0.1", args.port), Handler) as server:
        server.serve_forever()
