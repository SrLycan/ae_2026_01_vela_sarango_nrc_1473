# ADR-006: Backend partido en dos microservicios (`users` + `spotsapp`) detrás de nginx

## Contexto

La entrega final de Arquitectura Empresarial exige explícitamente un monorepo con al
menos dos microservicios independientes (cada uno con su propia base de datos) detrás de
un único gateway (nginx), más un explorador de base de datos y un formato de logging
estandarizado en ambos. El backend hasta ahora era un único Spring Boot monolítico
(`spots-app-backend`, ver ADR-005) con una sola base de datos, expuesto directo.

## Decisión

Partir el backend en dos microservicios dentro de un monorepo (`ae_2026_01_vela_sarango_nrc_1473`):

- **`users`**: gestiona el perfil propio de cada usuario (nombre, email, teléfono),
  asociado 1 a 1 al `sub` de Cognito. Parte de la base entregada en clase
  (`guia_microservicio_users.md`), adaptada de H2 a Postgres real.
- **`spotsapp`**: todo lo que ya existía en `spots-app-backend` (spots, reviews, follows,
  media, categorías, feed, stats) — se mueve tal cual, sin reescribir su lógica de dominio.
- **`nginx`**: único servicio expuesto al host; `users` y `spotsapp` solo usan `expose`
  (visibles entre sí dentro de la red de Docker, no desde afuera).

Ambos microservicios validan tokens contra el **mismo** User Pool de Cognito — es un solo
login para los dos, no hay un tercer sistema de autenticación.

## Esto revierte ADR-002

ADR-002 ("sin tabla users") documentaba la decisión de que `spotsapp` no necesitaba una
tabla `users` propia porque toda la identidad vivía en el JWT de Cognito (`ownerUsername`
como el `sub`, sin FK a ninguna tabla local). Esa decisión seguía siendo válida para
`spotsapp` en aislamiento, pero la rúbrica exige un microservicio de usuarios real con su
propia base de datos — así que se crea, pero como un microservicio **separado**, no como
una tabla dentro de `spotsapp`. `spotsapp` en sí mismo sigue sin tener una tabla `users`
ni una FK hacia ella: si en algún momento necesitara datos de perfil (nombre para
mostrar, etc.), se los pide a `users` por HTTP (`GET /api/users/cognito/{cognitoId}`), no
por join de SQL. El principio de ADR-002 (identidad = JWT, no acoplar por FK) se mantiene
igual de válido *entre* microservicios que dentro de uno solo.

## Consecuencias

**Positivas**
- Cumple el requisito de dos microservicios + gateway + BD por servicio.
- `users` y `spotsapp` pueden evolucionar y desplegarse de forma independiente (aunque en
  la práctica, para esta entrega, se despliegan juntos vía un solo `docker-compose.yml`).
- Un solo Cognito User Pool = una sola gestión de usuarios/contraseñas para todo el sistema.

**Negativas**
- Si `spotsapp` alguna vez necesita datos de `users` (p. ej. mostrar el nombre del dueño de
  un spot en vez de su `sub`), esa llamada HTTP entre microservicios agrega latencia y un
  nuevo punto de falla que no existía en el monolito — se documentará como un ADR aparte si
  y cuando se implemente.
- Dos bases de datos, dos ciclos de build/despliegue, dos `Dockerfile` — más piezas móviles
  que mantener que el monolito original.
