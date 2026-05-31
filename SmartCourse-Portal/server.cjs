"use strict";

const fs = require("fs");
const http = require("http");
const path = require("path");

const root = __dirname;
const port = Number(process.env.PORT || 9527);
const types = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".jpg": "image/jpeg",
  ".png": "image/png"
};

http.createServer((request, response) => {
  const requestPath = decodeURIComponent(request.url.split("?")[0]);
  const normalized = requestPath === "/" ? "/index.html" : requestPath;
  const filePath = path.join(root, normalized);

  if (!filePath.startsWith(root)) {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }

  fs.readFile(filePath, (error, content) => {
    if (error) {
      response.writeHead(error.code === "ENOENT" ? 404 : 500);
      response.end(error.code === "ENOENT" ? "Not Found" : "Server Error");
      return;
    }

    response.writeHead(200, {
      "Cache-Control": "no-cache",
      "Content-Type": types[path.extname(filePath).toLowerCase()] || "application/octet-stream"
    });
    response.end(content);
  });
}).listen(port, () => {
  console.log(`SmartCourse portal: http://localhost:${port}`);
});
