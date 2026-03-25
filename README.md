# Prueba Técnica — Arquitectura Microservicios (SemiSenior)

Sistema bancario implementado con dos microservicios Spring Boot independientes que se comunican de forma asíncrona mediante RabbitMQ.

---

## Tabla de contenidos

1. [Arquitectura general](#arquitectura-general)
2. [Requisitos previos](#requisitos-previos)
3. [Estructura del proyecto](#estructura-del-proyecto)
4. [Opción A — Ejecución con Docker Compose (recomendada)](#opción-a--ejecución-con-docker-compose-recomendada)
5. [Opción B — Ejecución local en desarrollo](#opción-b--ejecución-local-en-desarrollo)
6. [Inicializar la base de datos con datos de ejemplo](#inicializar-la-base-de-datos-con-datos-de-ejemplo)
7. [Verificar que todo está en marcha](#verificar-que-todo-está-en-marcha)
8. [Pruebas con Postman — flujo completo](#pruebas-con-postman--flujo-completo)
9. [Ejecutar tests automatizados](#ejecutar-tests-automatizados)
10. [Variables de entorno](#variables-de-entorno)
11. [Comandos útiles de Docker](#comandos-útiles-de-docker)
12. [Solución de problemas](#solución-de-problemas)

---

## Arquitectura general

```
┌─────────────────────────────────────────────────────────────┐
│                        Cliente HTTP                         │
│                (Postman / curl / frontend)                   │
└───────────────┬─────────────────────────┬───────────────────┘
                │                         │
        :8081   ▼                 :8082   ▼
┌───────────────────────┐   ┌───────────────────────┐
│      ms-clientes      │   │      ms-cuentas        │
│  (Persona + Cliente)  │   │  (Cuenta + Movimiento) │
│                       │   │                       │
│  GET/POST/PUT/PATCH/  │   │  GET/POST/PUT/PATCH/  │
│  DELETE /api/clientes │   │  DELETE /api/cuentas  │
│                       │   │  DELETE /api/movimien │
│                       │   │  GET /api/reportes    │
└──────────┬────────────┘   └───────────┬───────────┘
           │  Publica eventos            │ Consume eventos
           │  cliente.*                  │ cliente.*
           │                             │
           └──────────┬──────────────────┘
                      ▼
          ┌───────────────────────┐
          │       RabbitMQ        │
          │  Exchange: clientes   │
          │  Queue: cuentas.cli.. │
          └───────────────────────┘

┌─────────────────────┐    ┌─────────────────────┐
│    clientes_db      │    │     cuentas_db       │
│  (PostgreSQL)       │    │  (PostgreSQL)        │
│  · persona          │    │  · cliente_ref       │
│  · cliente          │    │  · cuenta            │
│                     │    │  · movimiento        │
└─────────────────────┘    └─────────────────────┘
```

| Microservicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `ms-clientes` | 8081 | `clientes_db` | CRUD de Persona y Cliente, publica eventos |
| `ms-cuentas` | 8082 | `cuentas_db` | CRUD de Cuenta y Movimiento, reportes, consume eventos |

---

## Requisitos previos

| Herramienta | Versión mínima | Verificar |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker Desktop | 24+ | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Git | cualquier | `git --version` |

> **Windows:** Docker Desktop debe estar corriendo antes de ejecutar cualquier comando `docker`.

---

## Estructura del proyecto

```
├── ms-clientes/                 # Microservicio 1
│   ├── src/main/java/...
│   ├── src/test/java/...
│   ├── Dockerfile
│   └── pom.xml
├── ms-cuentas/                  # Microservicio 2
│   ├── src/main/java/...
│   ├── src/test/java/...
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml           # Orquestación completa
├── init-db.sql                  # Crea clientes_db y cuentas_db al iniciar Postgres
├── BaseDatos.sql                # DDL completo + datos de ejemplo del PDF
└── README.md
```

---

## Opción A — Ejecución con Docker Compose (recomendada)

Esta opción levanta **todo el stack** (PostgreSQL, RabbitMQ, ms-clientes, ms-cuentas) con un solo comando. No requiere tener Java ni Maven instalados globalmente.

### Paso 1 — Compilar los JARs

Antes de construir las imágenes Docker, genera los artefactos `.jar` de cada microservicio:

```bash
# Desde la raíz del proyecto
cd ms-clientes
mvn clean package -DskipTests
cd ..

cd ms-cuentas
mvn clean package -DskipTests
cd ..
```

> Si Maven no está en el PATH, usa la ruta completa:
> ```bash
> "C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.2.6.1/plugins/maven/lib/maven3/bin/mvn" clean package -DskipTests
> ```

### Paso 2 — Construir y levantar todos los servicios

```bash
# Desde la raíz del proyecto (donde está docker-compose.yml)
docker compose up --build
```

El flag `--build` reconstruye las imágenes Docker. Si ya las tienes construidas y solo quieres iniciar:

```bash
docker compose up
```

Para ejecutar en segundo plano (modo detached):

```bash
docker compose up --build -d
```

### Paso 3 — Verificar que los contenedores están corriendo

```bash
docker compose ps
```

Deberías ver algo similar a:

```
NAME             IMAGE                STATUS          PORTS
postgres_banco   postgres:16-alpine   Up (healthy)    0.0.0.0:5432->5432/tcp
rabbitmq_banco   rabbitmq:3.13-...    Up (healthy)    0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
ms_clientes      demo-ms-clientes     Up              0.0.0.0:8081->8081/tcp
ms_cuentas       demo-ms-cuentas      Up              0.0.0.0:8082->8082/tcp
```

### Paso 4 — Ver logs en tiempo real

```bash
# Todos los servicios
docker compose logs -f

# Solo un microservicio
docker compose logs -f ms-clientes
docker compose logs -f ms-cuentas
```

Señales de que los servicios están listos:

- **PostgreSQL:** `database system is ready to accept connections`
- **RabbitMQ:** `Server startup complete`
- **ms-clientes:** `Started MsClientesApplication in X seconds`
- **ms-cuentas:** `Started MsCuentasApplication in X seconds`

### Paso 5 — Detener los servicios

```bash
# Detener sin borrar datos
docker compose stop

# Detener y eliminar contenedores (mantiene el volumen de datos)
docker compose down

# Detener, eliminar contenedores Y eliminar el volumen de datos (reset completo)
docker compose down -v
```

---

## Opción B — Ejecución local en desarrollo

Útil cuando quieres hacer cambios en el código y relanzar rápido sin reconstruir imágenes.

### Paso 1 — Levantar solo la infraestructura con Docker

```bash
docker compose up postgres rabbitmq -d
```

Espera unos 15 segundos a que PostgreSQL y RabbitMQ estén listos:

```bash
docker compose ps
# postgres_banco y rabbitmq_banco deben mostrar "(healthy)"
```

### Paso 2 — Crear las bases de datos

```bash
docker exec -it postgres_banco psql -U postgres -c "CREATE DATABASE clientes_db;"
docker exec -it postgres_banco psql -U postgres -c "CREATE DATABASE cuentas_db;"
```

Verificar que se crearon:

```bash
docker exec -it postgres_banco psql -U postgres -c "\l"
```

### Paso 3 — Ejecutar ms-clientes

Abre una terminal y ejecuta:

```bash
cd ms-clientes
mvn spring-boot:run
```

Espera hasta ver:

```
Started MsClientesApplication in X.XXX seconds (process running for X.XXX)
```

### Paso 4 — Ejecutar ms-cuentas

Abre **otra terminal** (sin cerrar la anterior) y ejecuta:

```bash
cd ms-cuentas
mvn spring-boot:run
```

Espera hasta ver:

```
Started MsCuentasApplication in X.XXX seconds (process running for X.XXX)
```

> **Importante:** `ms-clientes` debe estar completamente iniciado antes de arrancar `ms-cuentas`, para que RabbitMQ ya tenga el exchange declarado cuando `ms-cuentas` intente crear la binding de su cola.

---

## Inicializar la base de datos con datos de ejemplo

El script `BaseDatos.sql` crea el esquema completo y carga los datos del PDF (Jose Lema, Marianela Montalvo, Juan Osorio con sus cuentas y movimientos).

> **Nota:** ejecuta este paso **después** de que PostgreSQL esté corriendo y las bases de datos `clientes_db` / `cuentas_db` estén creadas.

### Con Docker (recomendado)

```bash
# Copiar el script al contenedor
docker cp BaseDatos.sql postgres_banco:/BaseDatos.sql

# Ejecutar el script
docker exec -it postgres_banco psql -U postgres -f /BaseDatos.sql
```

### Local con psql instalado

```bash
psql -U postgres -h localhost -f BaseDatos.sql
```

### Qué crea el script

**`clientes_db`**

| Tabla | Descripción |
|---|---|
| `persona` | Datos personales: nombre, genero, edad, identificacion (UNIQUE), direccion, telefono |
| `cliente` | Extiende persona con: contrasena, estado — clave primaria compartida (JOINED) |

**`cuentas_db`**

| Tabla | Descripción |
|---|---|
| `cliente_ref` | Espejo local de clientes recibido via RabbitMQ |
| `cuenta` | numeroCuenta (UNIQUE), tipoCuenta, saldoInicial, saldoDisponible, estado, clienteId |
| `movimiento` | fecha, tipoMovimiento, valor, saldo (resultado), FK a cuenta |

---

## Verificar que todo está en marcha

Una vez iniciados los servicios, verifica que cada uno responde:

```bash
# ms-clientes — debe retornar [] (lista vacía si no hay datos aún)
curl http://localhost:8081/api/clientes

# ms-cuentas — debe retornar []
curl http://localhost:8082/api/cuentas

# RabbitMQ Management UI
# Abrir en el navegador: http://localhost:15672
# Usuario: guest | Contraseña: guest
```

---

## Pruebas con Postman — flujo completo

Importa la colección o ejecuta las peticiones en el siguiente orden.
La URL base es `http://{host}:{puerto}/api/{recurso}`.

---

### 1. Crear clientes — `POST /api/clientes` (ms-clientes :8081)

**Jose Lema**
```http
POST http://localhost:8081/api/clientes
Content-Type: application/json

{
  "nombre": "Jose Lema",
  "genero": "M",
  "edad": 30,
  "identificacion": "0011223344",
  "direccion": "Otavalo sn y principal",
  "telefono": "098254785",
  "contrasena": "1234",
  "estado": true
}
```

**Marianela Montalvo**
```http
POST http://localhost:8081/api/clientes
Content-Type: application/json

{
  "nombre": "Marianela Montalvo",
  "genero": "F",
  "edad": 28,
  "identificacion": "0055667788",
  "direccion": "Amazonas y NNUU",
  "telefono": "097548965",
  "contrasena": "5678",
  "estado": true
}
```

**Juan Osorio**
```http
POST http://localhost:8081/api/clientes
Content-Type: application/json

{
  "nombre": "Juan Osorio",
  "genero": "M",
  "edad": 35,
  "identificacion": "0099887766",
  "direccion": "13 junio y Equinoccial",
  "telefono": "098874587",
  "contrasena": "1245",
  "estado": true
}
```

> Guarda los `clienteId` que devuelve cada respuesta (1, 2, 3). Los necesitas en el siguiente paso.

**Respuesta esperada (201 Created):**
```json
{
  "clienteId": 1,
  "nombre": "Jose Lema",
  "genero": "M",
  "edad": 30,
  "identificacion": "0011223344",
  "direccion": "Otavalo sn y principal",
  "telefono": "098254785",
  "estado": true
}
```

---

### 2. Crear cuentas — `POST /api/cuentas` (ms-cuentas :8082)

> En este punto `ms-cuentas` ya debe haber recibido los eventos `cliente.creado` de RabbitMQ. Si recibes un error `"Cliente no encontrado"`, espera unos segundos y reintenta.

```http
POST http://localhost:8082/api/cuentas
Content-Type: application/json

{ "numeroCuenta": "478758", "tipoCuenta": "AHORRO",    "saldoInicial": 2000, "estado": true, "clienteId": 1 }
```
```http
POST http://localhost:8082/api/cuentas
Content-Type: application/json

{ "numeroCuenta": "225487", "tipoCuenta": "CORRIENTE",  "saldoInicial": 100,  "estado": true, "clienteId": 2 }
```
```http
POST http://localhost:8082/api/cuentas
Content-Type: application/json

{ "numeroCuenta": "495878", "tipoCuenta": "AHORRO",    "saldoInicial": 0,    "estado": true, "clienteId": 3 }
```
```http
POST http://localhost:8082/api/cuentas
Content-Type: application/json

{ "numeroCuenta": "496825", "tipoCuenta": "AHORRO",    "saldoInicial": 540,  "estado": true, "clienteId": 2 }
```
```http
POST http://localhost:8082/api/cuentas
Content-Type: application/json

{ "numeroCuenta": "585545", "tipoCuenta": "CORRIENTE", "saldoInicial": 1000, "estado": true, "clienteId": 1 }
```

---

### 3. Registrar movimientos — `POST /api/movimientos` (ms-cuentas :8082)

```http
POST http://localhost:8082/api/movimientos
Content-Type: application/json

{ "cuentaId": 1, "valor": -575 }
```
```http
POST http://localhost:8082/api/movimientos
Content-Type: application/json

{ "cuentaId": 2, "valor": 600 }
```
```http
POST http://localhost:8082/api/movimientos
Content-Type: application/json

{ "cuentaId": 3, "valor": 150 }
```
```http
POST http://localhost:8082/api/movimientos
Content-Type: application/json

{ "cuentaId": 4, "valor": -540 }
```

---

### 4. Probar saldo insuficiente (F3)

```http
POST http://localhost:8082/api/movimientos
Content-Type: application/json

{ "cuentaId": 4, "valor": -1 }
```

**Respuesta esperada (400 Bad Request):**
```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Saldo no disponible"
}
```

---

### 5. Reporte de estado de cuenta (F4)

```http
GET http://localhost:8082/api/reportes?fechaInicio=2022-02-01&fechaFin=2022-02-28&clienteId=2
```

**Respuesta esperada (200 OK):**
```json
[
  {
    "fecha": "10/2/2022",
    "cliente": "Marianela Montalvo",
    "numeroCuenta": "225487",
    "tipo": "Corriente",
    "saldoInicial": 100.00,
    "estado": true,
    "movimiento": 600.00,
    "saldoDisponible": 700.00
  },
  {
    "fecha": "8/2/2022",
    "cliente": "Marianela Montalvo",
    "numeroCuenta": "496825",
    "tipo": "Ahorro",
    "saldoInicial": 540.00,
    "estado": true,
    "movimiento": -540.00,
    "saldoDisponible": 0.00
  }
]
```

---

### 6. Otros endpoints disponibles

```http
# Listar todos los clientes
GET http://localhost:8081/api/clientes

# Obtener cliente por ID
GET http://localhost:8081/api/clientes/1

# Actualizar cliente (completo)
PUT http://localhost:8081/api/clientes/1
Content-Type: application/json
{ "nombre": "Jose Lema", "identificacion": "0011223344", "contrasena": "nuevaPass", "estado": true }

# Actualizar cliente (parcial)
PATCH http://localhost:8081/api/clientes/1
Content-Type: application/json
{ "telefono": "099999999" }

# Eliminar cliente
DELETE http://localhost:8081/api/clientes/1

# Listar cuentas
GET http://localhost:8082/api/cuentas

# Listar movimientos
GET http://localhost:8082/api/movimientos

# Obtener movimiento por ID
GET http://localhost:8082/api/movimientos/1
```

---

### 7. Verificar manejo de errores

```http
# 404 — cliente no encontrado
GET http://localhost:8081/api/clientes/9999
```
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Cliente no encontrado con id: 9999" }
```

```http
# 400 — identificacion duplicada
POST http://localhost:8081/api/clientes
{ "nombre": "Otro", "identificacion": "0011223344", "contrasena": "pass", "estado": true }
```
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Ya existe un cliente con identificación: 0011223344" }
```

```http
# 400 — campo obligatorio faltante
POST http://localhost:8081/api/clientes
{ "nombre": "Sin identificacion", "contrasena": "pass" }
```
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "La identificación es obligatoria" }
```

---

## Ejecutar tests automatizados

```bash
# ms-clientes — 7 tests (3 entidad, 2 servicio, 2 controller)
cd ms-clientes
mvn test

# ms-cuentas — 6 tests (2 servicio, 2 controller, 2 integración con H2)
cd ms-cuentas
mvn test

# Ejecutar un test específico
mvn test -Dtest=ClienteControllerTest
mvn test -Dtest=ReporteControllerIntegrationTest
```

**Resultado esperado:**

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0   ← ms-clientes
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0   ← ms-cuentas
[INFO] BUILD SUCCESS
```

> Los tests de `ms-cuentas` usan H2 en memoria (perfil `test`) y **no requieren** PostgreSQL ni RabbitMQ en marcha.

---

## Variables de entorno

Ambos microservicios leen su configuración de variables de entorno con valores por defecto para desarrollo local:

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_USER` | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `postgres` | Contraseña de PostgreSQL |
| `RABBITMQ_HOST` | `localhost` | Host de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Contraseña de RabbitMQ |

En Docker Compose, estas variables se inyectan automáticamente apuntando a los nombres de servicio internos (`postgres`, `rabbitmq`).

Para desarrollo local, los defaults apuntan a `localhost`, por lo que no necesitas setear nada si usas los puertos por defecto.

---

## Comandos útiles de Docker

```bash
# Ver logs de un servicio específico
docker compose logs -f ms-clientes
docker compose logs -f ms-cuentas
docker compose logs -f postgres
docker compose logs -f rabbitmq

# Entrar a la consola de PostgreSQL
docker exec -it postgres_banco psql -U postgres

# Conectarse a clientes_db directamente
docker exec -it postgres_banco psql -U postgres -d clientes_db

# Conectarse a cuentas_db directamente
docker exec -it postgres_banco psql -U postgres -d cuentas_db

# Consultar tablas
docker exec -it postgres_banco psql -U postgres -d clientes_db -c "SELECT * FROM persona;"
docker exec -it postgres_banco psql -U postgres -d cuentas_db  -c "SELECT * FROM cuenta;"
docker exec -it postgres_banco psql -U postgres -d cuentas_db  -c "SELECT * FROM movimiento;"

# Reconstruir solo un servicio (tras cambios en el código)
docker compose up --build ms-clientes
docker compose up --build ms-cuentas

# Ver uso de recursos
docker stats

# Limpiar todo (contenedores, imágenes, volúmenes)
docker compose down -v
docker system prune -f
```

---

## Solución de problemas

### `ms-cuentas` devuelve "Cliente no encontrado" al crear una cuenta recién creada en ms-clientes

**Causa:** el evento `cliente.creado` aún no fue procesado por RabbitMQ.
**Solución:** espera 2-3 segundos y reintenta. En el log de `ms-cuentas` deberías ver:
```
ClienteRef actualizado: id=1, nombre=Jose Lema
```

---

### Error al compilar: `Could not find artifact` o descarga lenta

**Causa:** primera compilación descarga todas las dependencias (~200 MB).
**Solución:** espera a que termine. Las siguientes compilaciones son instantáneas (caché Maven en `~/.m2`).

---

### Puerto ya en uso: `Address already in use: 8081` o `8082`

**Causa:** otro proceso usa ese puerto.
**Solución:**
```bash
# Windows — encontrar el proceso
netstat -ano | findstr :8081
# Terminar el proceso (reemplaza PID con el número encontrado)
taskkill /PID <PID> /F
```

---

### Docker Compose: `Service 'ms-clientes' failed to build`

**Causa:** el archivo `.jar` no existe porque no se compiló con Maven.
**Solución:** ejecuta `mvn clean package -DskipTests` dentro de `ms-clientes/` y `ms-cuentas/` antes de `docker compose up --build`.

---

### RabbitMQ Management UI no carga en `localhost:15672`

**Causa:** el contenedor está iniciando.
**Solución:** espera 30 segundos y recarga. Verifica con:
```bash
docker compose ps rabbitmq
# Debe mostrar "(healthy)"
```

---

### `ms-cuentas` no recibe eventos aunque RabbitMQ está corriendo

**Causa:** la cola `cuentas.clientes.queue` no existe si `ms-cuentas` no logró crearla al iniciar.
**Solución:** reinicia `ms-cuentas`:
```bash
docker compose restart ms-cuentas
```
Verifica en RabbitMQ UI (`http://localhost:15672` → Queues) que aparece `cuentas.clientes.queue`.

---

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje |
| Spring Boot | 3.3.4 | Framework principal |
| Spring Data JPA | — | Persistencia ORM |
| Spring AMQP | — | Mensajería con RabbitMQ |
| Spring Validation | — | Validación de DTOs |
| PostgreSQL | 16 | Base de datos relacional |
| RabbitMQ | 3.13 | Message broker asíncrono |
| Lombok | — | Reducción de boilerplate |
| JUnit 5 + Mockito | — | Pruebas unitarias |
| H2 | — | Base de datos en memoria para tests |
| Docker + Compose | 24+ | Contenedores y orquestación |
| Maven | 3.9 | Gestión de dependencias y build |
