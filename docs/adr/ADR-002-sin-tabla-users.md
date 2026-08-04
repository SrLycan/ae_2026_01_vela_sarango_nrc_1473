# ADR-002: No mantener una tabla `users` propia en PostgreSQL

> **Actualización (ver [ADR-006](./ADR-006-microservicios-users-spotsapp.md)):** esta decisión
> se mantiene sin cambios *dentro de* `spotsapp` — sigue sin existir una tabla `users` ni una FK
> hacia ella en este servicio. Lo que cambió es que ahora existe una tabla `users` en un
> microservicio **separado** (`users`, con su propia base de datos), no dentro de `spotsapp`. El
> razonamiento de este ADR (identidad = JWT, no acoplar por FK) sigue aplicando igual entre
> microservicios que dentro de uno solo.

## Contexto

El sistema necesita identificar al propietario de un `Spot`, al autor de una `Review`, y las
relaciones de `Follow` entre usuarios. La autenticación se delega a AWS Cognito, que ya almacena
usuario, email, y grupos (roles). Existe la alternativa clásica de duplicar esos datos en una
tabla `users` local sincronizada con Cognito.

## Decisión

**No se crea una tabla `users` en PostgreSQL.** Las entidades que necesitan referenciar a un
usuario (`Spot.ownerUser`, `Review.user`, `Follow.followerUsername` / `Follow.followingUsername`)
almacenan directamente el identificador de Cognito (`sub` o `username`) como una columna simple
(String), no como una FK a una tabla local. El perfil (nombre, avatar, stats) se resuelve
combinando ese identificador con el JWT/Cognito y con los cálculos de `StatsService`.

## Consecuencias

**Positivas**
- Se elimina el problema de sincronización usuario↔Cognito (webhooks, jobs, drift de datos).
- Cognito sigue siendo la única fuente de verdad de identidad, evitando duplicidad.
- Menos una tabla y sus migraciones, menos joins en las queries de dominio.

**Negativas**
- No hay integridad referencial (FK) a nivel de base de datos entre `Spot.ownerUser` y un usuario
  real; la validez del username depende de que el JWT sea válido en el momento de la operación.
- Consultas que requieran "datos enriquecidos del usuario" (nombre para mostrar, avatar) deben
  resolverse en la capa de aplicación, no con un simple JOIN SQL.
- Si en el futuro se necesitan atributos de usuario propios de la app (p. ej. biografía, foto de
  perfil dentro de la app), habrá que reconsiderar esta decisión y crear una tabla ligera de perfil.
