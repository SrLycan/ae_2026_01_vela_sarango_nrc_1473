# Spots App — Backend (Arquitectura Empresarial, entrega final)

Repo: `ae_2026_01_vela_sarango_nrc_1473`

Dos microservicios (Kotlin + Spring Boot), cada uno con su propia base de datos
PostgreSQL, detrás de un único gateway nginx. La app móvil (repo aparte,
`spots-app-mobile`) es el único cliente de este backend.

```
├── users/          Microservicio de perfiles de usuario (base de la clase, extendida)
├── spotsapp/        Microservicio de dominio: spots, reviews, follows, media, categorías
├── nginx/           Gateway — único servicio expuesto al host
├── docker-compose.yml
├── .env.example
└── docs/            ADRs, requisitos, arquitectura de nube, plan de despliegue, BMC, plan financiero
```

## Por qué dos microservicios y no uno

`users` resuelve **quién es** el usuario autenticado (perfil: nombre, email, teléfono,
asociado 1 a 1 al `sub` de Cognito). `spotsapp` resuelve **qué hace** ese usuario dentro
del dominio del proyecto (crear spots, reseñar, seguir a otros usuarios). Cada uno tiene
su propia base de datos — ninguno lee la tabla del otro directamente; si `spotsapp`
necesitara datos de perfil, se los pediría a `users` por HTTP
(`GET /api/users/cognito/{cognitoId}`), no por SQL cruzado.

Los dos confían en el **mismo** User Pool de Cognito — un solo login (un solo par de
tokens) sirve para hablar con cualquiera de los dos microservicios.

## Correr todo el stack

```bash
cp .env.example .env
# edita .env con tus valores reales (COGNITO_ISSUER_URI, contraseñas de BD, bucket S3)
docker compose up -d --build
```

Esto levanta: nginx (puerto 80, el único expuesto), `users` + su Postgres, `spotsapp` +
su Postgres, y pgAdmin (127.0.0.1:5050, ver más abajo). Prueba con:

```bash
curl http://localhost/api/users/me          # -> 401 sin token, como debe ser
curl http://localhost/spots                 # -> 200, lista pública de spots aprobados
```

Todo el tráfico entra por el puerto 80 de nginx — `users` y `spotsapp` **no** tienen
puertos publicados al host, solo `expose` dentro de la red de Docker (ver
`docker-compose.yml`). Antes, la app apuntaba directo a `spotsapp:8080`; ahora que hay
gateway, debe apuntar al puerto 80 (ver la nota en el README de `spots-app-mobile`).

## Explorador de base de datos (pgAdmin)

Se accede a través de nginx, en `/pgadmin/` — funciona igual en local que ya desplegado en EC2:

```
http://localhost/pgadmin/          (local)
http://<IP_PUBLICA_EC2>/pgadmin/   (desplegado)
```

Entra con `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` de tu `.env` — usa una contraseña real ahí,
porque a diferencia de las bases de datos (que solo escuchan en `127.0.0.1` dentro de la
instancia), pgAdmin **sí** queda alcanzable desde cualquier IP a través de nginx; su login
es la única protección. La primera vez, agrega los dos servidores manualmente (**Add New
Server**):

| Campo | `users-db` | `spotsapp-db` |
|---|---|---|
| Host | `users-db` | `spotsapp-db` |
| Port | `5432` | `5432` |
| Database | `users` | `spotsapp` |
| Username | el de `USERS_DB_USERNAME` | el de `SPOTSAPP_DB_USERNAME` |

(El host es el nombre del *servicio* de Docker, no `localhost` — pgAdmin corre dentro de
la misma red `ae-net` y resuelve esos nombres directo.)

## Desarrollo local (sin Docker, un microservicio a la vez)

Cada microservicio tiene su propio `README`/guía dentro de su carpeta:
- [`users/guia_microservicio_users.md`](./users/guia_microservicio_users.md)
- [`spotsapp/README.md`](./spotsapp/README.md)

## Despliegue

Ver [`docs/despliegue-ec2.md`](./docs/despliegue-ec2.md) — la mecánica no cambia (EC2 +
Docker Compose + IAM Role para S3), solo que ahora `docker compose up -d --build` levanta
5 servicios en vez de 2, y el puerto público pasa a ser el 80 (nginx) en vez del 8080
directo al backend.

## Pendiente (ver conversación / próximos pasos)

- Formato de logging estándar (línea única con `sub=`, `event=`, etc.) en ambos micros.
- Colección de Postman versionada con los flujos punta a punta (login → crear spot → etc.).
- Medir y completar cobertura de tests al 100% en ambos micros.
