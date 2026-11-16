// Genera el código TOTP (RFC 6238, 6 dígitos, 30 s) de un secreto en base32.
// Solo para pruebas locales: node scripts/totp.mjs <SECRETO>
import { createHmac } from 'node:crypto'

const ALFABETO = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

function base32(texto) {
  let bits = ''
  for (const c of texto.replace(/[\s=]/g, '').toUpperCase()) bits += ALFABETO.indexOf(c).toString(2).padStart(5, '0')
  const bytes = []
  for (let i = 0; i + 8 <= bits.length; i += 8) bytes.push(parseInt(bits.slice(i, i + 8), 2))
  return Buffer.from(bytes)
}

const secreto = process.argv[2]
if (!secreto) {
  console.error('Uso: node scripts/totp.mjs <SECRETO_BASE32>')
  process.exit(1)
}
const contador = Buffer.alloc(8)
contador.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30000)))
const hmac = createHmac('sha1', base32(secreto)).update(contador).digest()
const desplazamiento = hmac[hmac.length - 1] & 0x0f
const codigo = (hmac.readUInt32BE(desplazamiento) & 0x7fffffff) % 1_000_000
console.log(String(codigo).padStart(6, '0'))
