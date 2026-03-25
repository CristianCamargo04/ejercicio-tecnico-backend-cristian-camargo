-- ================================================================
-- BaseDatos.sql
-- Prueba Técnica — Arquitectura Microservicios (SemiSenior)
-- ================================================================
-- Microservicios:
--   ms-clientes  → base: clientes_db  (tablas: persona, cliente)
--   ms-cuentas   → base: cuentas_db   (tablas: cliente_ref, cuenta, movimiento)
--
-- Uso con psql:
--   psql -U postgres -f BaseDatos.sql
--
-- Uso con Docker:
--   docker exec -i postgres_banco psql -U postgres -f /BaseDatos.sql
-- ================================================================


-- ================================================================
-- 0. CREAR BASES DE DATOS
-- ================================================================

-- Terminar conexiones activas antes de crear (útil en re-ejecuciones)
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname IN ('clientes_db', 'cuentas_db')
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS clientes_db;
DROP DATABASE IF EXISTS cuentas_db;

CREATE DATABASE clientes_db
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE   = 'en_US.utf8'
         TEMPLATE   = template0;

CREATE DATABASE cuentas_db
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE   = 'en_US.utf8'
         TEMPLATE   = template0;


-- ================================================================
-- 1. BASE DE DATOS: clientes_db
--    Microservicio: ms-clientes (puerto 8081)
--    Entidades JPA: Persona (base), Cliente (extends Persona — JOINED)
-- ================================================================

\connect clientes_db

-- ---------------------------------------------------------------
-- 1.1 ESQUEMA — tabla persona
--     Entidad: com.example.msclientes.domain.entity.Persona
--     @Inheritance(strategy = InheritanceType.JOINED)
-- ---------------------------------------------------------------
CREATE TABLE persona (
    id             BIGSERIAL    PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    genero         VARCHAR(10),
    edad           INTEGER,
    identificacion VARCHAR(20)  NOT NULL UNIQUE,
    direccion      VARCHAR(200),
    telefono       VARCHAR(15)
);

-- ---------------------------------------------------------------
-- 1.2 ESQUEMA — tabla cliente
--     Entidad: com.example.msclientes.domain.entity.Cliente
--     @PrimaryKeyJoinColumn(name = "cliente_id")
--     Hereda: id, nombre, genero, edad, identificacion, direccion, telefono
-- ---------------------------------------------------------------
CREATE TABLE cliente (
    cliente_id BIGINT       PRIMARY KEY
                            REFERENCES persona(id) ON DELETE CASCADE,
    contrasena VARCHAR(100) NOT NULL,
    estado     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- 1.3 ÍNDICES
-- ---------------------------------------------------------------
CREATE INDEX idx_persona_identificacion ON persona(identificacion);
CREATE INDEX idx_cliente_estado         ON cliente(estado);

-- ---------------------------------------------------------------
-- 1.4 DATOS DE EJEMPLO (casos de uso del PDF)
--
--   Nombres          Dirección                Teléfono    Contraseña  Estado
--   Jose Lema        Otavalo sn y principal   098254785   1234        True
--   Marianela Montalvo Amazonas y NNUU        097548965   5678        True
--   Juan Osorio      13 junio y Equinoccial   098874587   1245        True
-- ---------------------------------------------------------------
INSERT INTO persona (id, nombre, genero, edad, identificacion, direccion, telefono)
VALUES
    (1, 'Jose Lema',           'M', 30, '0011223344', 'Otavalo sn y principal',  '098254785'),
    (2, 'Marianela Montalvo',  'F', 28, '0055667788', 'Amazonas y NNUU',         '097548965'),
    (3, 'Juan Osorio',         'M', 35, '0099887766', '13 junio y Equinoccial',  '098874587');

INSERT INTO cliente (cliente_id, contrasena, estado)
VALUES
    (1, '1234', TRUE),
    (2, '5678', TRUE),
    (3, '1245', TRUE);

-- Resetear secuencia para que los próximos INSERTs vía API no colisionen
SELECT setval('persona_id_seq', (SELECT MAX(id) FROM persona));


-- ================================================================
-- 2. BASE DE DATOS: cuentas_db
--    Microservicio: ms-cuentas (puerto 8082)
--    Entidades JPA: ClienteRef, Cuenta, Movimiento
-- ================================================================

\connect cuentas_db

-- ---------------------------------------------------------------
-- 2.1 ESQUEMA — tabla cliente_ref
--     Entidad: com.example.mscuentas.domain.entity.ClienteRef
--     Tabla local de referencia, poblada por eventos RabbitMQ
--     (NO tiene relación directa con clientes_db)
-- ---------------------------------------------------------------
CREATE TABLE cliente_ref (
    cliente_id BIGINT       PRIMARY KEY,
    nombre     VARCHAR(100)
);

-- ---------------------------------------------------------------
-- 2.2 ESQUEMA — tabla cuenta
--     Entidad: com.example.mscuentas.domain.entity.Cuenta
--     tipoCuenta: enum TipoCuenta { AHORRO, CORRIENTE }
-- ---------------------------------------------------------------
CREATE TABLE cuenta (
    id               BIGSERIAL    PRIMARY KEY,
    numero_cuenta    VARCHAR(20)  NOT NULL UNIQUE,
    tipo_cuenta      VARCHAR(20)  NOT NULL
                                  CHECK (tipo_cuenta IN ('AHORRO', 'CORRIENTE')),
    saldo_inicial    DECIMAL(15,2) NOT NULL,
    saldo_disponible DECIMAL(15,2) NOT NULL,
    estado           BOOLEAN      NOT NULL DEFAULT TRUE,
    cliente_id       BIGINT       NOT NULL
                                  REFERENCES cliente_ref(cliente_id)
);

-- ---------------------------------------------------------------
-- 2.3 ESQUEMA — tabla movimiento
--     Entidad: com.example.mscuentas.domain.entity.Movimiento
--     tipoMovimiento: enum TipoMovimiento { DEPOSITO, RETIRO }
--     valor  : positivo = depósito, negativo = retiro
--     saldo  : saldo_disponible de la cuenta DESPUÉS del movimiento
-- ---------------------------------------------------------------
CREATE TABLE movimiento (
    id              BIGSERIAL    PRIMARY KEY,
    fecha           TIMESTAMP    NOT NULL,
    tipo_movimiento VARCHAR(20)  NOT NULL
                                 CHECK (tipo_movimiento IN ('DEPOSITO', 'RETIRO')),
    valor           DECIMAL(15,2) NOT NULL,
    saldo           DECIMAL(15,2) NOT NULL,
    cuenta_id       BIGINT       NOT NULL
                                 REFERENCES cuenta(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 2.4 ÍNDICES
-- ---------------------------------------------------------------
CREATE INDEX idx_cuenta_cliente_id          ON cuenta(cliente_id);
CREATE INDEX idx_cuenta_numero              ON cuenta(numero_cuenta);
CREATE INDEX idx_movimiento_cuenta_id       ON movimiento(cuenta_id);
CREATE INDEX idx_movimiento_fecha           ON movimiento(fecha);
-- Índice compuesto para la query del reporte (F4)
CREATE INDEX idx_movimiento_cuenta_fecha    ON movimiento(cuenta_id, fecha);

-- ---------------------------------------------------------------
-- 2.5 DATOS DE EJEMPLO — cliente_ref
--     Refleja lo que ms-cuentas recibiría vía eventos RabbitMQ
--     desde ms-clientes al crear cada cliente
-- ---------------------------------------------------------------
INSERT INTO cliente_ref (cliente_id, nombre)
VALUES
    (1, 'Jose Lema'),
    (2, 'Marianela Montalvo'),
    (3, 'Juan Osorio');

-- ---------------------------------------------------------------
-- 2.6 DATOS DE EJEMPLO — cuenta
--
--   Num Cuenta  Tipo       Saldo Inicial  Cliente
--   478758      Ahorro     2000           Jose Lema
--   225487      Corriente  100            Marianela Montalvo
--   495878      Ahorros    0              Juan Osorio
--   496825      Ahorros    540            Marianela Montalvo
--   585545      Corriente  1000           Jose Lema  (creada en caso de uso 3)
--
--   saldo_disponible = saldo_inicial aplicados los movimientos del caso de uso 4
-- ---------------------------------------------------------------
INSERT INTO cuenta (id, numero_cuenta, tipo_cuenta, saldo_inicial, saldo_disponible, estado, cliente_id)
VALUES
    (1, '478758', 'AHORRO',    2000.00, 1425.00, TRUE, 1),
    (2, '225487', 'CORRIENTE',  100.00,  700.00, TRUE, 2),
    (3, '495878', 'AHORRO',       0.00,  150.00, TRUE, 3),
    (4, '496825', 'AHORRO',     540.00,    0.00, TRUE, 2),
    (5, '585545', 'CORRIENTE', 1000.00, 1000.00, TRUE, 1);

-- ---------------------------------------------------------------
-- 2.7 DATOS DE EJEMPLO — movimiento
--
--   Caso de uso 4 del PDF:
--   Cuenta 478758 (Jose Lema)         → Retiro de 575   → saldo 1425
--   Cuenta 225487 (Marianela)         → Depósito de 600 → saldo 700
--   Cuenta 495878 (Juan Osorio)       → Depósito de 150 → saldo 150
--   Cuenta 496825 (Marianela)         → Retiro de 540   → saldo 0
--
--   Caso de uso 5 del PDF (listado de movimientos Marianela por fechas):
--   10/02/2022  Marianela  225487  Corriente  Depósito  600  saldo 700
--   08/02/2022  Marianela  496825  Ahorros    Retiro   -540  saldo 0
-- ---------------------------------------------------------------
INSERT INTO movimiento (id, fecha, tipo_movimiento, valor, saldo, cuenta_id)
VALUES
    (1, '2022-02-08 10:00:00', 'RETIRO',   -575.00, 1425.00, 1),
    (2, '2022-02-10 10:00:00', 'DEPOSITO',  600.00,  700.00, 2),
    (3, '2022-02-09 10:00:00', 'DEPOSITO',  150.00,  150.00, 3),
    (4, '2022-02-08 11:00:00', 'RETIRO',   -540.00,    0.00, 4);

-- Resetear secuencias
SELECT setval('cuenta_id_seq',     (SELECT MAX(id) FROM cuenta));
SELECT setval('movimiento_id_seq', (SELECT MAX(id) FROM movimiento));


-- ================================================================
-- 3. VERIFICACIÓN (consultas de comprobación)
-- ================================================================

\connect clientes_db

\echo '--- clientes_db: personas y clientes ---'
SELECT
    p.id,
    p.nombre,
    p.identificacion,
    p.direccion,
    p.telefono,
    c.contrasena,
    c.estado
FROM persona p
JOIN cliente c ON c.cliente_id = p.id
ORDER BY p.id;

\connect cuentas_db

\echo '--- cuentas_db: cuentas por cliente ---'
SELECT
    cu.numero_cuenta,
    cu.tipo_cuenta,
    cu.saldo_inicial,
    cu.saldo_disponible,
    cu.estado,
    cr.nombre AS cliente
FROM cuenta cu
JOIN cliente_ref cr ON cr.cliente_id = cu.cliente_id
ORDER BY cu.id;

\echo '--- cuentas_db: movimientos (caso de uso 5 del PDF) ---'
SELECT
    m.fecha::DATE                  AS fecha,
    cr.nombre                      AS cliente,
    cu.numero_cuenta,
    cu.tipo_cuenta,
    cu.saldo_inicial,
    cu.estado,
    m.valor                        AS movimiento,
    m.saldo                        AS saldo_disponible
FROM movimiento m
JOIN cuenta     cu ON cu.id         = m.cuenta_id
JOIN cliente_ref cr ON cr.cliente_id = cu.cliente_id
WHERE cr.nombre = 'Marianela Montalvo'
ORDER BY m.fecha;
