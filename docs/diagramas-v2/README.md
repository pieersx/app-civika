# Diagramas de Cívika (v2)

El archivo **[civika-diagramas.drawio](civika-diagramas.drawio)** es la fuente editable: un libro de draw.io con
**6 páginas**. Cada página también está exportada como imagen `.drawio.svg`, que GitHub muestra tal cual y lleva dentro
el diagrama editable. Los diagramas originales siguen en [../diagramas](../diagramas) sin cambios.

| # | Diagrama | Qué muestra |
|---|---|---|
| 1 | [Sistema](01-sistema.drawio.svg) | Las piezas (app Android, panel web, Supabase, servicios externos) y cómo se conectan |
| 2 | [Infraestructura y despliegue](02-infraestructura.drawio.svg) | CI, Vercel, migraciones de Supabase, APK firmado y secretos |
| 3 | [Arquitectura hexagonal](03-hexagonal.drawio.svg) | Dominio, puertos y adaptadores |
| 4 | [Flujo de un reporte](04-flujo-reporte.drawio.svg) | Del ciudadano a la entidad, por actor |
| 5 | [Estados del incidente](05-estados-incidente.drawio.svg) | Máquina de estados y quién hace cada transición |
| 6 | [Modelo de datos](06-modelo-datos.drawio.svg) | Las 17 tablas con columnas, claves y relaciones |

El detalle de cada decisión está en [arquitectura.md](../arquitectura.md).

## Cómo editarlos

1. Abre `civika-diagramas.drawio` con la extensión *Draw.io Integration* de VS Code o en app.diagrams.net.
2. Para refrescar la imagen de una página: *Archivo → Exportar como → SVG*, marca «Incluir una copia de mi diagrama» y
   guarda con el nombre de la página.
3. No uses un editor de imágenes para guardar un `.drawio.svg`: borra el diagrama editable.
