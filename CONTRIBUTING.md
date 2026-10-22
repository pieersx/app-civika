# Cómo contribuir a Cívika

## 1. Toma una tarea

Elige un issue del tablero del sprint actual y asígnatelo. Si no existe un issue para lo que vas a hacer, créalo
primero e indica el RF que cubre.

## 2. Crea tu rama

```bash
git switch main
git pull --ff-only
git switch -c tipo/descripcion-corta
```

Valores de `tipo`: `feat`, `fix`, `test`, `docs`, `refactor` o `chore`. Ejemplo: `feat/camara-reporte`.

## 3. Commits

- Formato: `tipo: descripción`, en español y con un máximo de 60 caracteres. Ejemplo: `feat: captura de hasta tres fotos`.
- Cada commit debe contener un solo cambio lógico.
- Añade un cuerpo al mensaje solo cuando el motivo del cambio no sea obvio.

## 4. Pull request

1. Antes de abrir el PR, sincroniza tu rama con `main`: `git fetch origin && git rebase origin/main`.
2. Abre el PR contra `main` con `Closes #N` en la descripción.
3. Necesitas **1 revisión aprobada de otra persona** y el **CI en verde**. La auto-revisión no cuenta: el autor
   nunca aprueba su propio PR. El tech lead revisa los PRs de los demás; si el PR es suyo, lo revisa otro
   colaborador. Si no hay otro revisor disponible, aplica la excepción del tech lead (apartado 4.1). El CI solo
   corre si el PR toca código o configuración de build; un PR solo de documentación no lo ejecuta y no lo necesita.
4. Quien aprueba el PR —o el tech lead en la excepción del apartado 4.1— lo fusiona en modo fast-forward, sin crear
   un commit de merge:
   ```bash
   git switch main && git pull --ff-only
   git merge --ff-only origin/tipo/descripcion-corta
   git push
   ```
   Si git rechaza el `--ff-only`, el autor tiene que rebasar su rama otra vez. Después se borra la rama.

### 4.1 Excepción del tech lead

Si el PR es de Pieers y no hay otro revisor disponible:

1. El CI debe estar verde. Si el PR solo toca documentación, el CI no aplica.
2. Deja un comentario en el PR: «Sin revisión independiente disponible; aplico la excepción del tech lead».
3. Fusiona con `--ff-only` como indica el paso 4. No marques el PR como aprobado: GitHub no permite autoaprobar.
4. En cuanto haya otro colaborador, vuelve a la regla general.

## 5. Dependencias

Antes de añadir o actualizar una librería, revisa su documentación vigente, con context7 si usas Claude Code (ver la
[guía de herramientas](docs/guia-herramientas.md)), y fija la versión en `gradle/libs.versions.toml`. Así nadie
copia ejemplos de APIs viejas.

## 6. Reglas que no se negocian

- Nunca se hace push directo a `main`.
- Nunca se suben secretos: `.env`, `local.properties`, `google-services.json` ni claves de Supabase.
  El repo es **público**.
- La clave `service_role` de Supabase solo se guarda en los secretos de las Edge Functions. Nunca va en `androidApp/` ni en
  `webPanel/`: el APK y el código Wasm del navegador se pueden descompilar.
- No se suben datos personales reales, como fotos con rostros o correos de usuarios de prueba.
