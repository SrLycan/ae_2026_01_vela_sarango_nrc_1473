# Despliegue del backend en EC2 (plan gratuito, sin dominio)

Runbook operativo para llevar `docker-compose.yml` de la laptop a una instancia EC2 real.
El contexto y las decisiones de diseño están en [`arquitectura-nube.md`](./arquitectura-nube.md#4-despliegue-real-usado-en-este-proyecto-ec2-standalone-plan-gratuito).

## 0. Qué necesitas antes de empezar

- Cuenta de AWS (free tier).
- El User Pool de Cognito ya creado (el mismo que ya usan en desarrollo) y su `issuer-uri`.
- Un bucket S3 ya creado (el mismo que usan en desarrollo, o uno nuevo para "producción").
- Este repo (`spots-app-backend`) accesible para clonar (público, o con una llave desplegada/PAT si es privado).

## 1. Lanzar la instancia EC2

1. EC2 → **Launch instance**.
2. AMI: **Ubuntu Server 24.04 LTS** (free tier eligible, la que aparece marcada así en el buscador de AMIs).
3. Tipo de instancia: **t2.micro** o **t3.micro** (free tier). Con 1 GiB de RAM corriendo
   Postgres + Spring Boot a la vez, agrega swap (paso 4) o vas a ver OOM kills.
4. Par de llaves: crea uno nuevo o reutiliza uno existente (lo necesitas para SSH).
5. **Security Group** — crea uno nuevo con estas reglas de entrada:

   | Tipo | Puerto | Origen | Para qué |
   |---|---|---|---|
   | SSH | 22 | Tu IP (`My IP`, **no** `0.0.0.0/0`) | Administración |
   | Custom TCP | 8080 | `0.0.0.0/0` | La app Android necesita llegar al backend desde cualquier red |

   **No abras el 5432 (Postgres) al público** — `docker-compose.yml` ya lo publica solo en
   `127.0.0.1:5432` (ver paso 5), así que ni hace falta la regla.

6. **Elastic IP**: asígnale una IP elástica a la instancia (EC2 → Elastic IPs → Allocate →
   Associate). Sin esto, la IP pública cambia cada vez que paras/arrancas la instancia y tendrías
   que recompilar el APK cada vez — con la mayoría de las cuentas nuevas, una Elastic IP asociada
   a una instancia corriendo no tiene costo adicional dentro del free tier; si la desasocias o la
   instancia queda detenida, sí empieza a cobrar por hora, así que no la dejes "suelta".

## 2. IAM Role para S3 (sin credenciales estáticas)

`S3Config` (backend) ya usa la cadena de credenciales por defecto del SDK de AWS — no hace falta
ninguna variable `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` si la instancia tiene un Role con
permisos sobre el bucket. Es más simple y más seguro que credenciales estáticas en un `.env`.

1. IAM → **Roles** → **Create role** → tipo de entidad de confianza: **AWS service** → **EC2**.
2. Permisos: crea una policy acotada al bucket (reemplaza `TU_BUCKET`):

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:PutObject", "s3:GetObject"],
         "Resource": "arn:aws:s3:::TU_BUCKET/*"
       }
     ]
   }
   ```
3. Nombra el role (p. ej. `spotsapp-ec2-s3-role`) y créalo.
4. Vuelve a la instancia EC2 → **Actions → Security → Modify IAM role** → selecciona el role.

Con esto, en el `.env` de la instancia, `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` se dejan
**vacíos** (igual que ya están por defecto en `.env` de este repo) — el SDK v2 detecta que esas
variables no tienen un valor utilizable y sigue la cadena hasta el proveedor de credenciales de
instancia (IMDS), que resuelve automáticamente las credenciales temporales del Role.

## 3. Preparar la instancia (SSH)

```bash
ssh -i tu-llave.pem ubuntu@<IP_ELASTICA>

sudo apt-get update
sudo apt-get install -y ca-certificates curl

# Repo oficial de Docker para Ubuntu (trae Docker Engine + el plugin de Compose v2 juntos)
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
# cierra sesión y vuelve a entrar para que el grupo "docker" tome efecto
exit
```

```bash
ssh -i tu-llave.pem ubuntu@<IP_ELASTICA>
docker compose version   # confirma que el plugin quedó instalado
```

### 3.1 Swap (recomendado en t2/t3.micro — 1 GiB de RAM)

```bash
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
```

## 4. Clonar el repo y configurar `.env`

```bash
git clone <url-del-repo-spots-app-backend>.git
cd spots-app-backend
cp .env.example .env
nano .env   # completa COGNITO_ISSUER_URI, AWS_S3_BUCKET, AWS_REGION reales;
            # deja AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY VACÍOS (usa el IAM Role del paso 2)
            # cambia DB_PASSWORD por una contraseña real (no dejes "spotsapp")
```

## 5. `docker-compose.yml` para producción

Ya viene ajustado en este repo para este escenario (ver comentarios en el archivo), en particular:

- `db` publica el puerto solo en `127.0.0.1:5432:5432` (no accesible desde fuera de la instancia,
  ni siquiera si alguien abriera el puerto en el Security Group por error).
- `backend` publica `8080:8080` (el único puerto que necesita estar abierto al público).

## 6. Levantar

```bash
docker compose up -d --build
docker compose logs -f backend    # confirma que arrancó sin errores (Ctrl+C para salir del log)
```

Prueba desde tu propia laptop (no desde la instancia):

```bash
curl http://<IP_ELASTICA>:8080/actuator/health   # o el endpoint de health que tenga el backend
```

## 7. Apuntar la app Android a la instancia

1. En `mobile/app/build.gradle.kts`, build type `release`, reemplaza `REPLACE_WITH_EC2_PUBLIC_IP`
   por la IP elástica real en `BASE_URL`.
2. En `mobile/app/src/main/res/xml/network_security_config.xml`, reemplaza el mismo placeholder
   por la misma IP (para que Android permita `http://` hacia esa IP específica).
3. Genera el APK de release y pruébalo contra el backend real.

## 8. Actualizar el backend después de un cambio

```bash
cd spots-app-backend
git pull
docker compose up -d --build
```

## Troubleshooting rápido

- **`docker compose logs backend` muestra error de JWT/issuer** → revisa que
  `COGNITO_ISSUER_URI` en `.env` sea exactamente el issuer del User Pool (incluye la región y el
  Pool ID) y que el backend pueda alcanzar `cognito-idp.<region>.amazonaws.com` (saliente, normalmente
  sin restricciones en el Security Group por defecto).
- **La subida de fotos falla con 403 desde el backend (no desde la app)** → el Role de IAM no
  tiene permiso sobre el bucket, o el bucket/región del `.env` no coinciden con los reales.
- **`docker compose up` se cuelga o el contenedor de `backend` muere solo** → probablemente OOM
  por RAM; confirma que el swap del paso 3.1 esté activo (`free -h`).
- **La app Android no conecta pero `curl` desde tu laptop sí funciona** → revisa que hiciste los
  dos reemplazos del paso 7 (BASE_URL *y* network_security_config.xml) y que reconstruiste el
  APK después de cambiarlos.
