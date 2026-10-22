## Qué cambia

<!-- Una o dos frases. ¿Qué puede hacer ahora el usuario o el equipo que antes no podía? -->

Closes #

**Requisitos:** <!-- RF-.., RNF-.. -->

## Cómo probarlo

1.
2.

<!-- Si cambia la interfaz, adjunta una captura o un GIF. -->

## Checklist (Definición de «terminado»)

- [ ] La rama está rebasada sobre `main` (`git rebase origin/main`)
- [ ] Hay pruebas unitarias para la lógica nueva y pasan
- [ ] El CI está en verde (si el PR toca código o configuración de build; los PRs solo de docs no lo ejecutan)
- [ ] Lo probé en un emulador o dispositivo con API 26 o superior, o en el navegador si es del panel
- [ ] No hay secretos ni datos personales reales en el diff
- [ ] `revisor-codigo` revisó el diff antes del commit (y `/security-review` si toca `supabase/` o auth)
- [ ] El reviewer asignado no es el autor; si no hubo revisor, el tech lead dejó constancia de la excepción
- [ ] Si cambió una decisión, actualicé `docs/arquitectura.md` o `docs/diseno/direccion-visual.md`
