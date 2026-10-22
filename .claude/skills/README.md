# Skills del proyecto

`.agents/skills` es la copia canónica para OpenCode, Codex, Cursor y otros agentes. `.claude/skills` tiene una copia
real para Claude Code (sin symlinks, para que funcione en Windows).

## Actualizar

```bash
npx skills update
cp -a .agents/skills/. .claude/skills/
```

En Windows PowerShell:

```powershell
Copy-Item .agents\skills\* .claude\skills -Recurse -Force
```

`skills-lock.json` fija las versiones de las skills instaladas con `npx`.

## Origen y licencias

- `brainstorming`, `writing-plans`, `test-driven-development`, `systematic-debugging`, `using-git-worktrees`,
  `finishing-a-development-branch` y `verification-before-completion`: MIT, © Jesse Vincent,
  https://github.com/obra/superpowers
- `frontend-design`: Apache-2.0, © Anthropic, https://github.com/anthropics/skills (ver `LICENSE.txt`)
- `find-skills`: MIT, © Vercel, https://github.com/vercel-labs/skills
- `documentation-writer`: MIT, © GitHub, https://github.com/github/awesome-copilot
- `supabase` y `supabase-postgres-best-practices`: MIT, © Supabase, https://github.com/supabase/agent-skills
- `commit`, `explicar`, `mejorar-skill` y `mockup-ui`: skills personales de Pieers para Cívika.

El texto completo de las licencias MIT está en [`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md).

Las skills `docx` y `pptx` no se copian al repo: su licencia de Anthropic no permite redistribuirlas. Se usan desde
la cuenta de cada integrante.
