# ZIPLOC SAS

## Plataforma de billeteras digitales

**Proyecto académico · Spring Boot · React + Vite · Estructuras de datos
· API REST**

------------------------------------------------------------------------

## Tabla de contenido

1.  [Descripción general](#1-descripción-general)
2.  [Arquitectura](#2-arquitectura)
3.  [Estructuras de datos aplicadas](#3-estructuras-de-datos-aplicadas)
4.  [Módulos principales](#4-módulos-principales)
5.  [Enumeraciones del dominio](#5-enumeraciones-del-dominio)
6.  [Endpoints de la API](#6-endpoints-de-la-api)
7.  [Guía rápida de uso](#7-guía-rápida-de-uso)
8.  [Ejecución y pruebas](#8-ejecución-y-pruebas)
9.  [Resumen de estructuras y
    complejidad](#9-resumen-de-estructuras-y-complejidad)
10. [Consideraciones de
    documentación](#10-consideraciones-de-documentación)

------------------------------------------------------------------------

## 1. Descripción general

ZIPLOC SAS es una plataforma académica de billeteras digitales
desarrollada con **Spring Boot** para el backend y **React + Vite** para
el frontend. El proyecto integra operaciones financieras, administración
de usuarios, recompensas, notificaciones, operaciones programadas y
herramientas de analítica.

Uno de sus objetivos académicos es demostrar la aplicación de
estructuras de datos para administrar información y resolver operaciones
de manera organizada y eficiente.

### Funcionalidades principales

-   Registro y autenticación de usuarios.
-   Creación y administración de billeteras digitales.
-   Operaciones financieras: recargas, retiros, transferencias y
    reversiones.
-   Consulta de saldos e historiales de transacciones.
-   Operaciones programadas mediante una cola de prioridad.
-   Sistema de recompensas y niveles de fidelización.
-   Notificaciones y alertas por usuario.
-   Analítica de movimientos mediante grafos y reportes.
-   Registro de eventos de auditoría.

> **Persistencia:** la documentación de origen describe tanto
> estructuras de datos en memoria como integración con MongoDB. La
> distribución exacta de responsabilidades entre ambas capas debe
> contrastarse con la configuración y los repositorios actuales del
> proyecto.

------------------------------------------------------------------------

## 2. Arquitectura

La aplicación se organiza por capas para separar la recepción de
solicitudes, las reglas de negocio, las estructuras de datos y los
modelos del dominio.

### 2.1. Flujo general

``` text
Cliente HTTP
    |
    v
+----------------------+
|      Controller      |
| Recibe solicitudes   |
| HTTP y responde      |
+----------+-----------+
           |
           v
+----------------------+
|       Service        |
| Valida reglas        |
| Coordina operaciones |
+----------+-----------+
           |
      +----+----------------------+
      |                           |
      v                           v
+----------------------+  +----------------------+
|  PlataformaContext   |  |     DomainModel      |
| Gestiona estructuras |  | Usuario              |
| y datos de trabajo   |  | Billetera            |
|                      |  | Transaccion          |
| TablaUsuarios        |  | OperacionProgramada  |
| TablaBilleteras      |  | Alerta               |
| GestorTransacciones  |  | Beneficio            |
| ColaProgramadas      |  | EventoAuditoria      |
| GrafoTransferencias  |  +----------------------+
| ArbolFidelizacion    |
+----------------------+
```

### 2.2. Responsabilidades por capa

  -----------------------------------------------------------------------
  Capa                                Responsabilidad
  ----------------------------------- -----------------------------------
  `api/controller`                    Recibir solicitudes REST, validar
                                      entradas básicas y delegar
                                      operaciones.

  `api/dto`                           Definir los objetos utilizados para
                                      recibir y entregar información
                                      mediante la API.

  `application/service`               Implementar reglas de negocio y
                                      coordinar las estructuras de datos.

  `domain/model`                      Representar las entidades
                                      principales del sistema.

  `domain/enums`                      Definir estados, tipos y categorías
                                      del dominio.

  `domain/structures`                 Contener las implementaciones de
                                      estructuras de datos.

  `infrastructure`                    Agrupar componentes de
                                      persistencia, repositorios y tareas
                                      programadas, según la estructura
                                      descrita.
  -----------------------------------------------------------------------

### 2.3. Flujo de una transferencia

``` text
Cliente
  |
  | POST /api/transacciones/transferir
  v
TransaccionController
  |
  | Valida la solicitud y delega
  v
TransaccionService
  |
  +--> Verifica billeteras, saldo y estado
  |
  +--> BilleteraService
  |      Descuenta el saldo de origen
  |      y acredita el saldo de destino
  |
  +--> GestorTransacciones
  |      Registra el historial y la reversión
  |
  +--> GrafoTransferencias
  |      Registra la relación entre usuarios
  |
  +--> SistemaRecompensas
  |      Calcula puntos y actualiza el nivel
  |
  +--> EvaluadorRiesgo
  |      Analiza patrones y registra auditoría
  |
  +--> NotificacionService
         Registra la alerta correspondiente
  |
  v
Respuesta de la API
```

### 2.4. Decisiones de diseño documentadas

  -----------------------------------------------------------------------
  Decisión                            Justificación indicada
  ----------------------------------- -----------------------------------
  Estructuras de datos en memoria     Permiten demostrar la
                                      implementación y el comportamiento
                                      de las estructuras de datos.

  Servicios de Spring (`@Service`)    Separan la lógica de negocio de los
                                      controladores y permiten la
                                      inyección de dependencias.

  `PlataformaContext` centralizado    Busca evitar la duplicación del
                                      estado compartido entre servicios.

  Excepciones controladas             `ApiExceptionHandler` centraliza y
                                      estandariza las respuestas de
                                      error.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 3. Estructuras de datos aplicadas

Esta sección reúne las estructuras descritas para el proyecto, su
propósito y las operaciones que permiten realizar.

### 3.1. Listas

**Uso documentado:** historiales de transacciones por billetera,
beneficios canjeados, operaciones procesadas y reportes por periodo.

**Justificación:** permiten conservar el orden de los elementos y
recorrer la información para generar consultas, filtros e informes.

Ejemplo conceptual de un historial:

``` text
[TX-001] -> [TX-002] -> [TX-003] -> [TX-004]
 más antigua                         más reciente
```

### 3.2. Pila para reversión de transacciones

`GestorTransacciones` mantiene una pila de transacciones reversibles.

**Principio de funcionamiento:** LIFO (*Last In, First Out*). La última
transacción incorporada es la primera que se extrae para su reversión.

``` text
       +----------+
 TOPE  |  TX-004  |  <- siguiente elemento
       +----------+
       |  TX-003  |
       +----------+
       |  TX-002  |
       +----------+
       |  TX-001  |
       +----------+
```

  Operación                      Complejidad habitual
  ---------------------------- ----------------------
  Insertar (`push`)                              O(1)
  Consultar el tope (`peek`)                     O(1)
  Extraer (`pop`)                                O(1)

La reversión debe aplicar las reglas de negocio correspondientes para
deshacer el movimiento financiero. La pila, por sí sola, no garantiza
que una transacción pueda revertirse: también deben verificarse su
estado y las condiciones definidas por el servicio.

### 3.3. Cola circular para notificaciones

`ColaNotificaciones` conserva un número limitado de alertas recientes
por usuario.

**Justificación:** una capacidad fija permite controlar el espacio
utilizado y reutilizar las posiciones del arreglo de manera circular.

Ejemplo conceptual con capacidad de cinco elementos:

``` text
Antes:  [A1] [A2] [A3] [A4] [A5]
Llega:  A6
Después:[A2] [A3] [A4] [A5] [A6]
```

En este ejemplo, al llenarse la cola, la alerta más antigua se descarta
para incorporar la nueva.

  Operación            Complejidad habitual
  ------------------ ----------------------
  Insertar                             O(1)
  Extraer                              O(1)
  Consultar frente                     O(1)

### 3.4. Cola de prioridad para operaciones programadas

`ColaProgramadas` se describe como un heap mínimo indexado por fecha de
ejecución y prioridad.

**Objetivo:** obtener primero la operación que corresponde ejecutar
según el criterio de orden definido.

Ejemplo conceptual:

``` text
             [08:00, prioridad 1]
               /              \
 [09:00, prioridad 2]   [10:30, prioridad 1]
          /
 [12:00, prioridad 3]
```

La documentación de origen indica que un valor de prioridad más bajo
representa mayor urgencia cuando las fechas son iguales.

  Operación                            Complejidad indicada
  ---------------------------------- ----------------------
  Insertar una operación                           O(log n)
  Extraer la siguiente operación                   O(log n)
  Consultar la siguiente operación                     O(1)

### 3.5. Árbol binario de búsqueda para fidelización

`ArbolFidelizacion` organiza usuarios según sus puntos acumulados.

**Uso documentado:** consultas ordenadas, rankings y búsquedas por rango
de puntos.

Ejemplo conceptual:

``` text
             [Usuario C · 850 pts]
             /                   \
 [Usuario A · 420 pts]    [Usuario E · 1200 pts]
          \                       /
   [Usuario B · 610 pts] [Usuario D · 990 pts]
```

En un árbol binario de búsqueda, la ubicación de cada nodo depende de la
clave de comparación. El rendimiento depende de la altura del árbol.

  Operación                                    Complejidad
  -------------------- -----------------------------------
  Búsqueda               O(log n) promedio; O(n) peor caso
  Inserción              O(log n) promedio; O(n) peor caso
  Recorrido ordenado                                  O(n)

> La documentación de origen lo identifica como árbol binario de
> búsqueda. No se debe describir como árbol balanceado a menos que la
> implementación incluya y aplique un mecanismo de balanceo.

### 3.6. Tablas hash para usuarios y billeteras

La documentación describe dos estrategias de resolución de colisiones:

  -----------------------------------------------------------------------
  Estructura              Estrategia indicada     Uso
  ----------------------- ----------------------- -----------------------
  `TablaUsuarios`         Encadenamiento separado Búsqueda por
                                                  identificador de
                                                  usuario.

  `TablaBilleteras`       Direccionamiento        Consulta y
                          abierto con sondeo      actualización de
                          lineal                  billeteras.
  -----------------------------------------------------------------------

**Justificación:** las tablas hash permiten realizar búsquedas por clave
en O(1) promedio, aunque el rendimiento puede degradarse por colisiones
o por un factor de carga elevado.

### 3.7. Grafo dirigido y ponderado de transferencias

`GrafoTransferencias` representa relaciones entre usuarios mediante
aristas dirigidas y ponderadas por el monto transferido.

Ejemplo conceptual:

``` text
[Usuario A] -- $50.000 --> [Usuario B]
     |
  $20.000
     |
     v
[Usuario C] <-- $15.000 -- [Usuario D]
```

Consultas descritas:

-   Identificar vecinos directos de un usuario.
-   Consultar relaciones de envío y recepción.
-   Obtener el monto transferido entre dos nodos.
-   Analizar conexiones y posibles ciclos.
-   Identificar usuarios con mayor actividad dentro de la red.

### 3.8. Ordenamiento de transacciones por valor

`GestorTransacciones.obtenerMayoresValores` se describe como el
mecanismo para ordenar transacciones por monto y consultar las de mayor
valor.

La complejidad depende del algoritmo de ordenamiento utilizado. Si se
emplea un algoritmo de comparación eficiente, como Merge Sort o Heap
Sort, el coste habitual es O(n log n). Es necesario verificar el método
real antes de atribuirle esa complejidad al proyecto.

------------------------------------------------------------------------

## 4. Módulos principales

El proyecto se divide en un backend desarrollado con Spring Boot y un
frontend desarrollado con React y Vite.

### 4.1. Backend --- Spring Boot

Estructura de paquetes documentada:

``` text
src/main/java/ZiplocSAS/
├── api/
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── package-info.java
│   ├── controller/
│   └── dto/
│       └── package-info.java
├── application/
│   ├── service/
│   └── package-info.java
├── domain/
│   ├── enums/
│   ├── model/
│   ├── structures/
│   └── package-info.java
├── infrastructure/
│   ├── persistence/
│   ├── repository/
│   ├── scheduler/
│   └── package-info.java
├── package-info.java
└── ProyectoFinalApplication.java
```

  -----------------------------------------------------------------------
  Paquete                             Función
  ----------------------------------- -----------------------------------
  `api/config`                        Configuración de la API, como CORS.

  `api/controller`                    Controladores y endpoints REST.

  `api/dto`                           Objetos de transferencia de datos.

  `application/service`               Servicios y reglas de negocio.

  `domain/enums`                      Enumeraciones del dominio.

  `domain/model`                      Modelos principales.

  `domain/structures`                 Estructuras de datos.

  `infrastructure/persistence`        Componentes de persistencia
                                      descritos en el proyecto.

  `infrastructure/repository`         Interfaces de acceso a datos.

  `infrastructure/scheduler`          Procesamiento automático de
                                      operaciones programadas.
  -----------------------------------------------------------------------

### 4.2. Persistencia --- MongoDB

La documentación de origen señala el uso de MongoDB como capa de
persistencia y de estructuras de datos propias para operaciones en
memoria.

  -----------------------------------------------------------------------
  Componente                          Responsabilidad descrita
  ----------------------------------- -----------------------------------
  `infrastructure/persistence/`       Documentos y mapeo de entidades.

  `infrastructure/repository/`        Repositorios de acceso a datos.

  `infrastructure/scheduler/`         Tareas para procesar operaciones
                                      programadas.
  -----------------------------------------------------------------------

La sincronización concreta entre las estructuras en memoria y MongoDB
debe confirmarse en las clases de persistencia y servicios.

### 4.3. Frontend --- React + Vite

Estructura de carpetas documentada:

``` text
ziplocSAS_Front/
├── src/
│   ├── api/
│   ├── auth/
│   ├── components/
│   ├── hooks/
│   ├── interfaces/
│   ├── pages/
│   ├── services/
│   ├── styles/
│   │   ├── global.css
│   │   └── variables.css
│   ├── App.jsx
│   └── main.jsx
├── .env
├── index.html
├── tailwind.config.js
├── postcss.config.js
└── vite.config.js
```

  Herramienta o carpeta   Uso
  ----------------------- ------------------------------------------------------
  React + Vite            Construcción y ejecución de la interfaz.
  Tailwind CSS            Estilos y utilidades visuales.
  `src/api/`              Configuración de Axios y manejo de solicitudes HTTP.
  `src/auth/`             Contexto de autenticación y sesión.
  `src/components/`       Componentes reutilizables.
  `src/hooks/`            Hooks personalizados.
  `src/pages/`            Vistas principales.
  `src/services/`         Funciones para consumir los servicios del backend.
  `src/styles/`           Estilos globales y variables visuales.

------------------------------------------------------------------------

## 5. Enumeraciones del dominio

La documentación compartida identifica las siguientes enumeraciones:

  -----------------------------------------------------------------------
  Enumeración                         Valores documentados
  ----------------------------------- -----------------------------------
  `TipoTransaccion`                   `RECARGA`, `RETIRO`,
                                      `TRANSFERENCIA`, `PAGO_PROGRAMADO`

  `EstadoTransaccion`                 `COMPLETADA`, `PENDIENTE`,
                                      `FALLIDA`, `PROCESANDO`

  `NivelUsuario`                      `BRONCE`, `PLATA`, `ORO`, `PLATINO`

  `TipoBilletera`                     `AHORRO`, `CORRIENTE`, `INVERSION`
  -----------------------------------------------------------------------

Los valores deben coincidir con las enumeraciones existentes en el
código fuente. Si una clase enum contiene otros valores, debe prevalecer
la implementación real.

------------------------------------------------------------------------

## 6. Endpoints de la API

La documentación de origen establece `/api` como prefijo general de la
API.

### 6.1. Usuarios --- `/api/usuarios`

  -------------------------------------------------------------------------
  Método                  Ruta                      Descripción
  ----------------------- ------------------------- -----------------------
  POST                    `/api/usuarios`           Registrar un usuario.

  POST                    `/api/usuarios/login`     Autenticar un usuario.

  GET                     `/api/usuarios`           Listar usuarios.

  GET                     `/api/usuarios/{id}`      Consultar un usuario
                                                    por ID.

  PUT                     `/api/usuarios/{id}`      Actualizar datos del
                                                    usuario.

  DELETE                  `/api/usuarios/{id}`      Eliminar un usuario.

  GET                     `/api/usuarios/ranking`   Consultar el ranking de
                                                    puntos, según la
                                                    documentación original.
  -------------------------------------------------------------------------

### 6.2. Billeteras --- `/api/billeteras`

  ---------------------------------------------------------------------------------------
  Método                  Ruta                                    Descripción
  ----------------------- --------------------------------------- -----------------------
  POST                    `/api/billeteras`                       Crear una billetera.

  GET                     `/api/billeteras/usuario/{usuarioId}`   Listar billeteras de un
                                                                  usuario.

  GET                     `/api/billeteras/{id}`                  Consultar una
                                                                  billetera.

  PUT                     `/api/billeteras/{id}`                  Actualizar una
                                                                  billetera.

  DELETE                  `/api/billeteras/{id}`                  Eliminar una billetera.
  ---------------------------------------------------------------------------------------

### 6.3. Transacciones --- `/api/transacciones`

  ----------------------------------------------------------------------------------------------
  Método                  Ruta                                           Descripción
  ----------------------- ---------------------------------------------- -----------------------
  POST                    `/api/transacciones/recargar`                  Recargar una billetera.

  POST                    `/api/transacciones/retirar`                   Retirar saldo.

  POST                    `/api/transacciones/transferir`                Transferir entre
                                                                         billeteras.

  GET                     `/api/transacciones/historial/{billeteraId}`   Consultar historial,
                                                                         según la documentación
                                                                         original.

  POST                    `/api/transacciones/{id}/revertir`             Solicitar la reversión
                                                                         de una transacción.
  ----------------------------------------------------------------------------------------------

### 6.4. Operaciones programadas --- `/api/programadas`

  -----------------------------------------------------------------------------
  Método                  Ruta                          Descripción
  ----------------------- ----------------------------- -----------------------
  POST                    `/api/programadas`            Crear una operación
                                                        programada.

  GET                     `/api/programadas`            Listar operaciones
                                                        pendientes, según la
                                                        documentación original.

  POST                    `/api/programadas/procesar`   Procesar operaciones
                                                        cuya fecha de ejecución
                                                        haya llegado.
  -----------------------------------------------------------------------------

### 6.5. Recompensas --- `/api/recompensas`

  ----------------------------------------------------------------------------------------
  Método                  Ruta                                     Descripción
  ----------------------- ---------------------------------------- -----------------------
  GET                     `/api/recompensas/{usuarioId}`           Consultar puntos y
                                                                   nivel, según la
                                                                   documentación original.

  POST                    `/api/recompensas/{usuarioId}/canjear`   Canjear un beneficio,
                                                                   según la documentación
                                                                   original.
  ----------------------------------------------------------------------------------------

### 6.6. Notificaciones --- `/api/notificaciones`

  -----------------------------------------------------------------------------------
  Método                  Ruta                                Descripción
  ----------------------- ----------------------------------- -----------------------
  GET                     `/api/notificaciones/{usuarioId}`   Consultar alertas
                                                              recientes, según la
                                                              documentación original.

  POST                    `/api/notificaciones/despachar`     Despachar una
                                                              notificación
                                                              manualmente.

  PUT                     `/api/notificaciones/{id}/leer`     Marcar una alerta como
                                                              leída.
  -----------------------------------------------------------------------------------

### 6.7. Analítica --- `/api/analitica`

  ------------------------------------------------------------------------------------------
  Método                  Ruta                                       Descripción
  ----------------------- ------------------------------------------ -----------------------
  GET                     `/api/analitica/reporte/{usuarioId}`       Consultar el reporte de
                                                                     un usuario.

  GET                     `/api/analitica/top-billeteras`            Consultar billeteras
                                                                     con mayor uso.

  GET                     `/api/analitica/usuarios-activos`          Consultar el resumen de
                                                                     usuarios activos.

  GET                     `/api/analitica/grafo/{usuarioId}`         Consultar el grafo de
                                                                     transferencias.

  GET                     `/api/analitica/auditoria/{usuarioId}`     Consultar eventos de
                                                                     auditoría.

  GET                     `/api/analitica/rendimiento/{usuarioId}`   Consultar métricas de
                                                                     rendimiento.
  ------------------------------------------------------------------------------------------

> **Importante:** estas tablas ordenan las rutas incluidas en el texto
> de origen; no constituyen una verificación automática contra los
> controladores actuales. Antes de publicar el README, compara cada ruta
> y método HTTP con las anotaciones `@RequestMapping`, `@GetMapping`,
> `@PostMapping`, `@PutMapping`, `@PatchMapping` y `@DeleteMapping` de
> los controladores.

------------------------------------------------------------------------

## 7. Guía rápida de uso

Los siguientes ejemplos corresponden a los cuerpos de solicitud
incluidos en la documentación original. Los nombres de campos,
identificadores y respuestas deben verificarse contra los DTO reales.

### 7.1. Registrar un usuario

**Solicitud**

``` http
POST /api/usuarios
Content-Type: application/json
```

``` json
{
  "nombre": "Laura Gomez",
  "email": "laura@example.com",
  "contrasena": "secreto123",
  "telefono": "3001234567"
}
```

**Respuesta ilustrativa documentada**

``` json
{
  "ok": true,
  "datos": {
    "id": "USR-abc123",
    "nombre": "Laura Gomez",
    "email": "laura@example.com",
    "nivel": "BRONCE",
    "puntos": 0
  }
}
```

### 7.2. Iniciar sesión

``` http
POST /api/usuarios/login
Content-Type: application/json
```

``` json
{
  "email": "laura@example.com",
  "contrasena": "secreto123"
}
```

### 7.3. Crear una billetera

``` http
POST /api/billeteras
Content-Type: application/json
```

``` json
{
  "nombre": "Ahorro",
  "tipo": "AHORRO",
  "saldo": 50000,
  "usuarioId": "USR-abc123"
}
```

Tipos mencionados: `AHORRO`, `CORRIENTE` e `INVERSION`.

### 7.4. Recargar saldo

``` http
POST /api/transacciones/recargar
Content-Type: application/json
```

``` json
{
  "valor": 20000,
  "billeteraDestinoId": "BIL-xyz789"
}
```

### 7.5. Retirar saldo

``` http
POST /api/transacciones/retirar
Content-Type: application/json
```

``` json
{
  "valor": 5000,
  "billeteraOrigenId": "BIL-xyz789"
}
```

### 7.6. Transferir entre billeteras

``` http
POST /api/transacciones/transferir
Content-Type: application/json
```

``` json
{
  "valor": 10000,
  "billeteraOrigenId": "BIL-origen",
  "billeteraDestinoId": "BIL-destino"
}
```

La documentación describe que una transferencia actualiza el grafo de
relaciones, calcula puntos de fidelización y genera una notificación.

### 7.7. Revertir una transacción

``` http
POST /api/transacciones/{id}/revertir
```

La operación solicita revertir una transacción identificada por su ID.
Según la descripción original, el sistema utiliza una pila para
gestionar las reversiones. La disponibilidad de la reversión depende de
las validaciones y reglas de negocio implementadas.

### 7.8. Programar una operación

``` http
POST /api/programadas
Content-Type: application/json
```

``` json
{
  "fechaEjecucion": "2026-05-12T08:00:00",
  "tipo": "PAGO_PROGRAMADO",
  "valor": 15000,
  "billeteraOrigenId": "BIL-origen",
  "billeteraDestinoId": "BIL-destino",
  "prioridad": 1
}
```

La documentación indica que la prioridad se utiliza para ordenar
operaciones con la misma fecha de ejecución y que un valor menor
representa mayor urgencia.

### 7.9. Procesar operaciones pendientes

``` http
POST /api/programadas/procesar
```

La operación solicita procesar los pagos cuya fecha de ejecución ya se
haya cumplido, respetando el criterio de orden de la cola de prioridad.

### 7.10. Consultar analítica

``` http
GET /api/analitica/reporte/{usuarioId}
GET /api/analitica/top-billeteras
GET /api/analitica/usuarios-activos
GET /api/analitica/grafo/{usuarioId}
GET /api/analitica/auditoria/{usuarioId}
GET /api/analitica/rendimiento/{usuarioId}
```

------------------------------------------------------------------------

## 8. Ejecución y pruebas

### 8.1. Requisitos

-   JDK compatible con la versión configurada en el proyecto.
-   Maven Wrapper incluido en el repositorio.
-   Node.js y npm para ejecutar el frontend.
-   MongoDB, si la configuración actual de la aplicación requiere este
    servicio.

> La documentación de origen menciona Java 26 y una ruta local de JDK
> específica. Esos datos dependen del equipo donde se ejecuta el
> proyecto; consulta `pom.xml` y la configuración local para determinar
> la versión requerida.

### 8.2. Configurar Java en PowerShell

Ejemplo basado en la ruta local que aparece en la documentación
original. Sustituye la ruta por la ubicación real de tu JDK:

``` powershell
$env:JAVA_HOME = "C:\ruta\de\tu\jdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

### 8.3. Iniciar el backend

Desde la carpeta raíz del proyecto:

``` powershell
.\mvnw.cmd spring-boot:run
```

La documentación original indica que el servidor utiliza por defecto:

``` text
http://localhost:8080
```

Y que el prefijo de la API es:

``` text
http://localhost:8080/api
```

### 8.4. Ejecutar las pruebas del backend

``` powershell
.\mvnw.cmd test
```

### 8.5. Iniciar el frontend

Desde la carpeta `ziplocSAS_Front`:

``` powershell
npm install
npm run dev
```

Vite mostrará en la terminal la dirección local donde quedó disponible
la aplicación.

### 8.6. Probar endpoints con cURL

Registrar un usuario:

``` bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Laura Gomez","email":"laura@example.com","contrasena":"secreto123","telefono":"3001234567"}'
```

Consultar el grafo de un usuario:

``` bash
curl http://localhost:8080/api/analitica/grafo/USR-abc123
```

------------------------------------------------------------------------

## 9. Resumen de estructuras y complejidad

La siguiente tabla consolida las estructuras y clases mencionadas en la
documentación de origen.

  ---------------------------------------------------------------------------------
  Estructura        Clase indicada          Módulo                Complejidad
                                                                  principal
  ----------------- ----------------------- --------------------- -----------------
  Lista enlazada    `ListaEnlazada<T>`      `domain.structures`   Inserción O(1),
                                                                  búsqueda O(n),
                                                                  según operación y
                                                                  posición.

  Pila              `PilaReversion`         `domain.structures`   `push` y `pop`:
                                                                  O(1).

  Cola circular     `ColaCircular<T>`       `domain.structures`   Inserción y
                                                                  extracción: O(1).

  Heap mínimo       `ColaProgramadas`       `domain.structures`   Inserción y
                                                                  extracción: O(log
                                                                  n); consulta del
                                                                  mínimo: O(1).

  Árbol BST         `ArbolFidelizacion`     `domain.structures`   Búsqueda O(log n)
                                                                  promedio; O(n)
                                                                  peor caso.

  Hash con          `TablaUsuarios`         `domain.structures`   Búsqueda O(1)
  encadenamiento                                                  promedio; O(n)
                                                                  peor caso.

  Hash con sondeo   `TablaBilleteras`       `domain.structures`   Búsqueda O(1)
  lineal                                                          promedio; puede
                                                                  degradarse con
                                                                  muchas
                                                                  colisiones.

  Grafo dirigido    `GrafoTransferencias`   `domain.structures`   Recorrido de
  ponderado                                                       vecinos
                                                                  proporcional al
                                                                  grado del nodo,
                                                                  según
                                                                  representación.
  ---------------------------------------------------------------------------------

------------------------------------------------------------------------

## 10. Consideraciones de documentación

Este README organiza la información técnica compartida para describir la
plataforma, sus módulos, estructuras de datos, rutas y procedimientos de
ejecución.

Antes de considerar esta documentación como una especificación
definitiva, conviene verificar los siguientes puntos directamente en el
repositorio:

1.  Que los nombres de clases y paquetes coincidan exactamente con los
    archivos existentes.
2.  Que las rutas, los métodos HTTP y los parámetros coincidan con los
    controladores.
3.  Que los ejemplos JSON correspondan a los DTO aceptados por cada
    endpoint.
4.  Que las estructuras descritas estén implementadas y sean utilizadas
    por los servicios.
5.  Que la versión de Java coincida con la configuración de `pom.xml`.
6.  Que la integración con MongoDB y el almacenamiento en memoria estén
    descritos de acuerdo con el comportamiento real.
7.  Que las complejidades indicadas correspondan a los algoritmos
    implementados.

**ZIPLOC SAS --- Proyecto académico · Estructuras de Datos · 2026**
