# Colección de Postman

`spotsapp.postman_collection.json` — cubre los dos microservicios (`users` y `spotsapp`),
a través de nginx (puerto 80, no el 8080 ni el 8686 directo).

## Importar

Postman → **Import** → arrastra `spotsapp.postman_collection.json`. No hace falta un
Environment aparte — las variables (`baseUrl`, `username`, etc.) ya vienen como variables
de la propia colección: clic derecho en la colección → **Edit** → pestaña **Variables**.

## Antes de correr nada

1. **`baseUrl`**: por defecto trae la IP del EC2 (`http://52.204.103.205`). Si cambia la IP
   (instancia recreada sin Elastic IP, por ejemplo), actualízala ahí.
2. **`username`** / **`password`**: un usuario real de tu User Pool de Cognito.
3. **Requisito de Cognito**: el request de login usa el flujo `USER_PASSWORD_AUTH` (usuario
   + contraseña directo, sin SRP) porque es el único que Postman puede hacer sin plugins
   extra. Tu **App Client** de Cognito necesita tenerlo habilitado:
   - Consola de AWS → **Cognito** → tu User Pool → **App clients** → el client (el mismo
     `cognitoClientId` que ya está en `amplifyconfiguration.json` / la variable de la
     colección) → **Authentication flows** → marca **ALLOW_USER_PASSWORD_AUTH** → guardar.
   - Esto es aparte de `USER_SRP_AUTH`, que es el que usa la app móvil (Amplify) — puedes
     tener los dos habilitados a la vez sin problema, uno no reemplaza al otro.

## Cómo correrla

1. Carpeta **Auth (Cognito) → Login**: corre ese request primero. Guarda el token
   automáticamente en la variable de colección `accessToken` — todo lo demás ya lo hereda
   (Bearer Token a nivel de colección), no hay que copiar/pegar nada a mano.
2. De ahí en adelante, corre las carpetas en el orden que quieras. Las que crean algo
   (`Crear spot`, `Crear categoría`, `Crear review`, `Presign`) guardan el id resultante en
   una variable de colección (`spotId`, `categoryId`, `reviewId`, `uploadUrl`/`publicUrl`)
   automáticamente, así que las siguientes requests que dependen de ese id ya funcionan solas.
3. **Media** es la única carpeta con un paso manual (subir el archivo real a S3) — lee la
   descripción de ese request específico, tiene los detalles (por qué no lleva
   `Authorization`, cómo debe coincidir el `Content-Type`, etc.).
4. Para probar los endpoints de **ADMIN** (aprobar/rechazar spots, borrar categorías, etc.):
   corre **Login** de nuevo con las credenciales de un usuario que sí tenga el grupo/rol
   `ADMIN` en Cognito, para pisar el `accessToken` con uno que sí tenga permisos — así
   puedes comparar en vivo el mismo endpoint con un token de USER (403) y uno de ADMIN (200).

## Qué NO cubre todavía

- Registro de usuario nuevo (`SignUp` + confirmación de código) — eso vive en Cognito
  directo, no en esta colección; se agregará si terminamos construyendo esa pantalla en mobile.
