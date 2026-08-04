# Matriz de endpoints — Autorización

> Base para `SecurityConfig` (Fase 6) y para los controllers (Fase 7). "Propiedad" significa que,
> aunque el endpoint requiera solo estar autenticado (USER o ADMIN), el service valida además que
> el recurso pertenezca al usuario del JWT (`ownerUsername` / `username`), lanzando `403` si no
> (`ForbiddenOperationException`) — ver Fase 5.

| Método | Endpoint                          | Acceso        | Notas |
|--------|------------------------------------|---------------|-------|
| GET    | `/categories`, `/categories/{id}` | Público       | RF-02 |
| POST   | `/categories`                     | ADMIN         | RF-02 |
| PUT    | `/categories/{id}`                | ADMIN         | RF-02 |
| DELETE | `/categories/{id}`                | ADMIN         | RF-02 |
| GET    | `/spots`, `/spots/{id}`           | Público       | Solo spots `APPROVED` (RF-03) |
| GET    | `/spots/me`                       | USER, ADMIN   | Spots propios, cualquier estado |
| POST   | `/spots`                          | USER, ADMIN   | Nace `PENDING` |
| PUT    | `/spots/{id}`                     | USER, ADMIN   | + propiedad |
| DELETE | `/spots/{id}`                     | USER, ADMIN   | + propiedad |
| PATCH  | `/spots/{id}/approve`             | ADMIN         | RF-04 |
| PATCH  | `/spots/{id}/reject`              | ADMIN         | RF-04 |
| GET    | `/spots/{id}/media`               | Público       | RF-05 |
| POST   | `/spots/{id}/media`               | USER, ADMIN   | + propiedad del spot |
| DELETE | `/media/{id}`                     | USER, ADMIN   | + propiedad del spot |
| GET    | `/spots/{id}/reviews`             | Público       | RF-06 |
| POST   | `/reviews`                        | USER, ADMIN   | Spot debe estar `APPROVED` |
| PUT    | `/reviews/{id}`                   | USER, ADMIN   | + propiedad |
| DELETE | `/reviews/{id}`                   | USER, ADMIN   | + propiedad |
| POST   | `/follows/{username}`             | USER, ADMIN   | RF-07 |
| DELETE | `/follows/{username}`             | USER, ADMIN   | RF-07 |
| GET    | `/follows/me/following`           | USER, ADMIN   | RF-07 |
| GET    | `/follows/me/followers`           | USER, ADMIN   | RF-07 |
| GET    | `/feed`                           | USER, ADMIN   | RF-08 |
| GET    | `/profile/{username}/stats`       | Público       | RF-08 — perfil/puntos públicos, como un leaderboard |

Cualquier endpoint no listado explícitamente queda denegado por defecto (`anyRequest().authenticated()`).

Sin token en un endpoint privado → `401`. Token válido pero rol/propiedad incorrectos → `403`
(ver RNF-01 y Fase 11, Paso 11.2).
