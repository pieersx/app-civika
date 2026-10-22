---
name: mejorar-skill
description: Revisa la conversación actual tras usar una skill o tras varias correcciones, y propone reglas nuevas para esa skill o para CLAUDE.md. Úsala cuando el usuario diga "mejora la skill", "aprende de esto" o escriba /mejorar-skill.
---

1. Identifica qué skill se usó (o si las correcciones aplican a CLAUDE.md).
2. Lista las correcciones que hizo el usuario en esta conversación.
3. Separa las **reglas generales** (sirven siempre) de las **puntuales** (solo esta vez). Descarta las puntuales.
4. Propón cambios concretos: texto exacto a añadir o quitar, y en qué archivo (`.agents/skills/<nombre>/SKILL.md` más su copia en `.claude/skills/`, o el `CLAUDE.md` del proyecto).
5. Si una skill hace varias cosas distintas, sugiere dividirla en skills más pequeñas.
6. Si ves que se repite el mismo código, propón guardarlo en `scripts/` dentro de la skill.
7. Aplica los cambios solo cuando el usuario los apruebe.
