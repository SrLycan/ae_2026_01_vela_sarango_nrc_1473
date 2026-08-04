# ADR-004: Uso de PATCH vs PUT en la API REST

## Contexto

La API expone operaciones de actualización sobre `Spot`, `Review` y otras entidades. Algunas
actualizaciones son totales (el cliente reemplaza el recurso completo, como al editar un spot
propio desde un formulario) y otras son parciales y específicas del dominio, como la aprobación
o rechazo de un spot por parte de un administrador (`approve()` / `reject()`), donde solo cambian
`status`, `rarity` y `pointsReward`.

## Decisión

- **`PUT`** se usa para reemplazos completos de un recurso por su propietario: `PUT /spots/{id}`,
  `PUT /reviews/{id}`. El cliente envía el recurso completo actualizado.
- **`PATCH`** se usa para transiciones de estado y actualizaciones parciales controladas por el
  servidor, típicamente acciones administrativas: `PATCH /spots/{id}/approve`,
  `PATCH /spots/{id}/reject`. El cliente envía solo los campos relevantes a esa acción (p. ej.
  `rarity`, `pointsReward`, motivo de rechazo).

## Consecuencias

**Positivas**
- Semántica HTTP más clara: `PUT` es idempotente y reemplaza el recurso; `PATCH` expresa una
  intención de negocio específica (aprobar, rechazar) sin obligar al cliente a reenviar el
  recurso completo.
- Los endpoints de `PATCH` pueden tener reglas de autorización y validación distintas
  (solo `ADMIN`) sin mezclarse con la edición normal del propietario (`PUT`, solo `ownerUser`).
- Facilita el versionado del historial de cambios de estado si en el futuro se audita cada acción.

**Negativas**
- Aumenta el número de endpoints (uno por acción, en vez de un único endpoint genérico de
  actualización), lo que requiere más controllers/métodos a mantener.
- El equipo debe ser disciplinado para no "colar" cambios de otros campos dentro de un `PATCH` de
  acción específica, o se pierde la claridad semántica que motivó esta decisión.
