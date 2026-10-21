---
name: commit
description: Revisa los cambios sin guardar, propone un mensaje de commit claro y lo crea tras mi confirmación. Úsala solo cuando el usuario escriba /commit.
disable-model-invocation: true
---

!`git status --short`

!`git diff HEAD --stat`

1. Si estás en `main` o `master`, propón crear una rama nueva antes de seguir.
2. Resume los cambios en 2–3 viñetas sencillas y avisa si ves secretos, `.env` o archivos que no deberían subirse.
3. Propón un mensaje con formato `tipo: descripción` (feat, fix, test, docs, refactor, chore), máx. 60 caracteres, p. ej. "fix: lanza error al dividir entre cero". Si `git log --oneline` muestra que el repositorio usa otra convención, sigue esa.
   Si el motivo del cambio no se ve en el código, añade un cuerpo de 1 a 3 líneas (máx. 72 caracteres por línea) que explique el porqué, no el cómo. Si el cambio es obvio, solo el asunto. Nunca añadas líneas de atribución ni `Co-Authored-By`.
4. Muéstrame el mensaje completo (asunto y cuerpo), espera mi "sí" y luego haz `git add` solo de los archivos relevantes y `git commit` (un `-m` para el asunto y otro para el cuerpo).
5. No hagas `git push` salvo que lo pida.
