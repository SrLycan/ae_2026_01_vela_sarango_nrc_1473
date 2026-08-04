# Requisitos — Spots App

## Requisitos Funcionales (RF)

| ID | Requisito | Descripción | Actor |
|----|-----------|-------------|-------|
| RF-01 | Autenticación de usuarios | El sistema debe permitir registro e inicio de sesión mediante AWS Cognito, emitiendo un JWT válido para consumir la API. | Usuario / Admin |
| RF-02 | Gestión de categorías | Un administrador puede crear, editar y eliminar categorías de spots (`Category`). Cualquier usuario puede listarlas. | Admin / Usuario |
| RF-03 | Publicación de spots | Un usuario autenticado puede crear un spot (`Spot`) con nombre, descripción, ubicación y categoría. El spot ingresa con estado `PENDING`. | Usuario |
| RF-04 | Moderación de spots | Un administrador puede aprobar o rechazar un spot pendiente. Al aprobar, asigna `rarity` (rareza) y `pointsReward` (puntos que otorga). | Admin |
| RF-05 | Gestión de multimedia | El propietario de un spot puede subir imágenes/video (`Media`) asociadas al spot mediante URL prefirmada de S3, y eliminarlas. | Usuario |
| RF-06 | Reseñas de spots | Un usuario autenticado puede reseñar (`Review`) un spot ya `APPROVED` (una única reseña por usuario y spot), editarla o eliminarla. | Usuario |
| RF-07 | Seguimiento social | Un usuario puede seguir (`Follow`) y dejar de seguir a otros usuarios, y consultar sus seguidores/seguidos. No puede seguirse a sí mismo. | Usuario |
| RF-08 | Feed y gamificación | El sistema debe mostrar un feed con la actividad reciente de los usuarios seguidos, y calcular puntos totales, nivel y medalla de cada usuario a partir de los spots que ha reseñado (sin tabla adicional de puntos). | Usuario |

## Requisitos No Funcionales (RNF)

| ID | Requisito | Descripción |
|----|-----------|-------------|
| RNF-01 | Seguridad | Todos los endpoints privados deben validar el JWT emitido por Cognito y aplicar autorización por rol (`USER`/`ADMIN`) según la matriz de endpoints. Sin token → `401`; rol incorrecto → `403`. |
| RNF-02 | Escalabilidad | El backend debe ser stateless (sin sesión en memoria) y desplegable en contenedores Docker detrás de un balanceador (ALB), permitiendo escalamiento horizontal. |
| RNF-03 | Persistencia y consistencia | La base de datos PostgreSQL debe garantizar integridad referencial (FKs) y constraints únicos (p. ej. una reseña por usuario/spot, un follow único por par de usuarios). Las migraciones deben ser versionadas con Flyway. |
| RNF-04 | Usabilidad móvil | La app Android debe mostrar estados `Loading`/`Success`/`Error` en cada pantalla que consuma la API, con validaciones de formulario y confirmaciones antes de operaciones destructivas (borrar spot, media o reseña). |

## Trazabilidad rápida RF ↔ Entidad

| Entidad | RF relacionados |
|---------|------------------|
| `Category` | RF-02 |
| `Spot` | RF-03, RF-04 |
| `Media` | RF-05 |
| `Review` | RF-06, RF-08 |
| `Follow` | RF-07, RF-08 |
