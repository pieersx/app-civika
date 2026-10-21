# Cívika

> Así como Waze convierte a los conductores en sensores del tráfico, Cívika convierte a los ciudadanos en sensores de su ciudad.

**Cívika** es una aplicación móvil Android, desarrollada en Kotlin, para reportar incidencias urbanas
(baches, residuos y barreras de accesibilidad) con foto y GPS. Asigna cada reporte a la entidad responsable,
consolida los reportes repetidos en un único incidente que otros ciudadanos pueden apoyar y permite seguir su atención hasta la solución.

Proyecto del **Grupo 10** del curso *Desarrollo de Sistemas Móviles* (UNMSM, FISI, semestre 2026-II).

## Stack

Todo el cliente está en **Kotlin Multiplatform**.

| Parte | Tecnología |
|---|---|
| Código común (`shared`) | Dominio y reglas de negocio, repositorios, supabase-kt sobre Ktor, Koin, kotlinx.serialization |
| App móvil (`androidApp`) | Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow), Room, WorkManager, CameraX, ubicación de Android, MapLibre con teselas de OpenFreeMap, ML Kit |
| Panel web (`webPanel`) | Compose Multiplatform para web (Kotlin/Wasm) |
| Backend | Supabase: PostgreSQL + PostGIS, Auth, Storage, Edge Functions |
| Notificaciones | Firebase Cloud Messaging |

## Estructura

```
app-civika/
├─ shared/      Kotlin común: dominio y datos
├─ androidApp/  App Android del ciudadano (Compose)
├─ webPanel/    Panel web de la entidad (Compose Web, Wasm)
├─ supabase/    Migraciones SQL, datos semilla, pruebas pgTAP y edge functions
└─ docs/        Arquitectura, diseño, plan, guía de herramientas y entregables
```

## Documentación

- [Entregables 1 y 2](docs/entregables-1-y-2.md): problema, ODS, antecedentes, requisitos y arquitectura.
- [Arquitectura](docs/arquitectura.md): contexto, modelo de datos, estados, permisos y decisiones técnicas (ADR).
- [Dirección visual](docs/diseno/direccion-visual.md): paleta, tipografía y tokens de diseño.
- [Plan de desarrollo](docs/plan-desarrollo.md): roles, sprints, riesgos y flujo de trabajo del equipo.
- [Guía de herramientas](docs/guia-herramientas.md): qué skills, plugins y MCP de Claude Code usar en el proyecto.
- [Cómo contribuir](CONTRIBUTING.md): ramas, commits y pull requests.

## Equipo

Emily Cerna · Valeria Flores · Keyla Sihuacollo · Pieers Pachas (tech lead) · Alex Chavez
