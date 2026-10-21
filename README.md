# Cívika

> Así como Waze convierte a los conductores en sensores del tráfico, Cívika convierte a los ciudadanos en sensores de su ciudad.

**Cívika** es una aplicación móvil Android, desarrollada en Kotlin, para reportar incidencias urbanas
(baches, residuos y barreras de accesibilidad) con foto y GPS. Asigna cada reporte a la entidad responsable,
consolida los reportes repetidos en un único incidente que otros ciudadanos pueden apoyar y permite seguir su atención hasta la solución.

Proyecto del **Grupo 10** del curso *Desarrollo de Sistemas Móviles* (UNMSM, FISI, semestre 2026-II).

## Stack

| Parte | Tecnología |
|---|---|
| App móvil | Kotlin, Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow), Room, Retrofit, WorkManager, CameraX, ubicación de Android, mapa sobre OpenStreetMap |
| Backend | Supabase: PostgreSQL + PostGIS, Auth, Storage, Edge Functions |
| Panel web | Vite + TypeScript + supabase-js |
| Notificaciones | Firebase Cloud Messaging |

## Estructura

```
app-civika/
├─ android/     App Android (Kotlin + Compose)
├─ supabase/    Migraciones SQL, datos semilla y edge functions
├─ panel-web/   Panel web de la entidad
└─ docs/        Entregables, plan de desarrollo y diseño
```

## Documentación

- [Entregables 1 y 2](docs/entregables-1-y-2.md): problema, ODS, antecedentes, requisitos y arquitectura.
- [Arquitectura](docs/arquitectura.md): contexto, modelo de datos, estados, permisos y decisiones técnicas (ADR).
- [Dirección visual](docs/diseno/direccion-visual.md): paleta, tipografía y tokens de diseño.
- [Plan de desarrollo](docs/plan-desarrollo.md): roles, sprints y flujo de trabajo del equipo.
- [Cómo contribuir](CONTRIBUTING.md): ramas, commits y pull requests.

## Equipo

Emily Cerna · Valeria Flores · Keyla Sihuacollo · Pieers Pachas (tech lead) · Alex Chavez
