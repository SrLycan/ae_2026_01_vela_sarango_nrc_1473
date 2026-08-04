# Convención de GitFlow — Spots App

Este proyecto sigue una versión simplificada de GitFlow, adecuada para un proyecto académico con
entregas por fase.

## Ramas principales

- **`main`** — Código estable, correspondiente a lo entregado/sustentado. Solo recibe merges desde
  `release/*` (o directamente desde `develop` al cierre de cada fase, en este proyecto académico).
- **`develop`** — Rama de integración. Todo el trabajo en curso de las fases del plan maestro se
  integra aquí antes de pasar a `main`.

## Ramas de apoyo

- **`feature/<nombre-corto>`** — Una rama por funcionalidad o paso del plan maestro, creada desde
  `develop`. Ejemplos: `feature/entidades-jpa`, `feature/spot-service`, `feature/cognito-security`,
  `feature/mobile-login`.
  - Se fusiona de vuelta a `develop` mediante Pull Request al completar el paso.
  - Nomenclatura sugerida ligada a las fases del plan: `feature/fase2-entidades`,
    `feature/fase5-services`, `feature/fase9-auth-mobile`, etc.
- **`release/<version>`** — Se crea desde `develop` cuando una fase mayor (p. ej. Fase 8 —
  Dockerización, o Fase 10 — pantallas móviles) queda lista para consolidarse. Sirve para
  estabilizar (fixes menores, documentación) antes de mergear a `main`.

## Flujo típico por paso del plan

1. Parado en `develop`, crear `feature/<paso>`.
2. Implementar el paso (código + tests si aplica).
3. Commit con mensaje descriptivo (ver convención abajo).
4. Merge (o PR) de `feature/<paso>` → `develop`.
5. Al cerrar una fase completa, merge de `develop` → `main` (o vía `release/*` si se desea mayor
   control) y tag opcional `fase-N`.

## Convención de mensajes de commit

Se usa un estilo tipo *Conventional Commits* simplificado:

```
<tipo>: <descripción corta>

tipo ∈ { feat, fix, chore, docs, test, refactor }
```

Ejemplos:
- `feat: agregar CategoryService con CRUD básico`
- `fix: validar propiedad del spot antes de eliminar`
- `docs: agregar ADR-003 sobre nivel calculado`
- `test: pruebas unitarias de ReviewService`
- `chore: configurar Flyway en application.yml`

## Tags

Se recomienda taggear `main` al cerrar cada fase mayor del plan, por ejemplo:
`git tag fase-2-entidades`, `git tag fase-7-api-completa`, `git tag fase-10-mobile-completo`.
