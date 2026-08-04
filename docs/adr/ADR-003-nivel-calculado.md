# ADR-003: Nivel y medalla del usuario se calculan, no se almacenan

## Contexto

La gamificación requiere mostrar, por usuario, sus puntos totales, un "nivel" y una "medalla"
derivados de la actividad (spots reseñados y `pointsReward` de esos spots). Una opción es
mantener columnas `totalPoints`, `level`, `badge` en algún registro persistente que se actualice
en cada reseña; otra es calcularlos en el momento de la consulta.

## Decisión

Nivel, medalla y puntos totales se **calculan en tiempo de consulta** dentro de `StatsService`,
mediante `SUM(pointsReward)` de los spots asociados a las `Review` del usuario, aplicando después
reglas de umbral (p. ej. rangos de puntos → nivel/medalla). No se persiste ningún valor de stats
en base de datos.

## Consecuencias

**Positivas**
- Una sola fuente de verdad (`Review` + `Spot.pointsReward`): imposible que el puntaje quede
  desincronizado del historial real de reseñas.
- No se requiere lógica de "recalcular y actualizar" cada vez que cambia un `pointsReward` (p. ej.
  si un admin corrige la rareza de un spot después de aprobarlo).
- Menos una tabla/columnas que migrar y mantener.

**Negativas**
- El cálculo de stats tiene costo en cada consulta (agregación SQL) en lugar de una simple
  lectura; a mayor escala puede requerir caché o vista materializada.
- Los umbrales de nivel/medalla quedan como lógica de aplicación (hardcodeada o configurable), no
  como datos versionados en base de datos.
