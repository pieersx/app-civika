import '@fontsource/bricolage-grotesque/600.css'
import '@fontsource/bricolage-grotesque/700.css'
import '@fontsource/atkinson-hyperlegible-next/400.css'
import '@fontsource/atkinson-hyperlegible-next/700.css'
import './styles/index.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { aplicarTema } from './lib/tema'

aplicarTema()

const raiz = document.getElementById('root')
if (!raiz) throw new Error('Falta #root en index.html')
createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
