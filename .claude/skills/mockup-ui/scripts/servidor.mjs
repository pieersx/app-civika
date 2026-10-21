// Servidor estático mínimo para probar un mockup en local (solo lectura, solo localhost).
// Uso: node servidor.mjs <carpeta> [puerto]   (el puerto por defecto es 4173)
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, resolve, sep } from 'node:path';

const ROOT = resolve(process.argv[2] || '.');
const BASE = ROOT.endsWith(sep) ? ROOT : ROOT + sep;
const PORT = Number(process.argv[3] || 4173);
if (!Number.isInteger(PORT) || PORT < 1 || PORT > 65535) {
  console.error('Puerto no válido:', process.argv[3]);
  process.exit(1);
}
const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp',
  '.gif': 'image/gif', '.ico': 'image/x-icon', '.woff2': 'font/woff2',
};

const server = createServer(async (req, res) => {
  try {
    let ruta = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    if (ruta.endsWith('/')) ruta += 'Main.dc.html';
    const file = resolve(join(ROOT, ruta));
    // Nunca salir de la carpeta servida (por ejemplo con ..%2f..%2f).
    if (file !== ROOT && !file.startsWith(BASE)) { res.writeHead(403).end('no'); return; }
    const body = await readFile(file);
    res.writeHead(200, { 'Content-Type': TYPES[extname(file)] || 'application/octet-stream' }).end(body);
  } catch (e) {
    const noExiste = e instanceof URIError || e?.code === 'ENOENT' || e?.code === 'EISDIR';
    res.writeHead(noExiste ? 404 : 500).end(noExiste ? 'no encontrado' : 'error del servidor');
  }
});
server.on('error', (e) => {
  console.error(e.code === 'EADDRINUSE' ? `El puerto ${PORT} ya está en uso.` : e.message);
  process.exit(1);
});
server.listen(PORT, '127.0.0.1', () => console.log('sirviendo', ROOT, 'en http://127.0.0.1:' + PORT));
