# Cambios aplicados a ZiplocSAS Wallet

## 1. Árbol de fidelización
- Reemplazada la implementación BST sin balanceo por un AVL real con rotaciones LL, RR, LR y RL.
- Clave de orden: puntos acumulados y, para empates, ID del usuario.
- Índice hash auxiliar por ID para localizar y actualizar usuarios sin recorrer todo el árbol.
- Los rankings y búsquedas por rango de `UsuarioService` ahora consultan el AVL.

## 2. Operaciones programadas
- El heap se reconstruye desde MongoDB antes de cada lote; no depende del contenido de memoria tras reiniciar Spring Boot.
- Las operaciones fallidas quedan pendientes y guardan `ultimoError`, que se muestra en la interfaz.
- El botón de procesamiento informa cuántas operaciones se ejecutaron, fallaron o si no había operaciones vencidas.
- Se valida el tipo, el monto, la fecha y las billeteras necesarias según la operación.
- Se añadió `operacionProgramadaId` a la transacción para reconocer reintentos y evitar duplicar el movimiento si la operación ya generó una transacción persistida.
- Se añadió recurrencia opcional: una vez, diaria, semanal o mensual.
- Las consultas y el procesamiento manual filtran por el usuario dueño de la billetera; el scheduler automático continúa procesando todas las cuentas.

## 3. Integridad recursiva
- Nuevo endpoint `GET /api/transacciones/integridad`.
- Revisa IDs, fechas, tipo, estado, monto, puntos y referencias de billeteras.
- Usa recursividad por división del historial para mantener baja la profundidad de llamadas.

## 4. Análisis de gasto y grafos
- Nuevo resumen `GET /api/analitica/patrones-gasto`, agrupado por tipo de billetera de origen.
- Nuevo grafo dirigido ponderado de billeteras, separado del grafo entre usuarios.
- Nuevo endpoint `GET /api/analitica/grafo-billeteras`.
- El cargador de MongoDB reconstruye ambos grafos al iniciar y excluye transacciones revertidas.
- Al revertir una transacción, se reconstruyen los grafos para retirar sus aristas del análisis.
- La pantalla de Analítica muestra el resumen de gasto por tipo de billetera.

## 5. Registro y errores
- `UsuarioDTO.activo` pasó de `boolean` a `Boolean` para aceptar `null` durante la deserialización JSON.
- El interceptor HTTP prioriza el detalle del backend (`error` / `detail`) antes del mensaje genérico.

## Validación realizada en este entorno
- Pruebas aisladas de AVL: inserción ordenada, actualización, consulta por rango, ranking y eliminación; correctas.
- Prueba aislada del heap de prioridad: orden por fecha y prioridad; correcta.
- Análisis sintáctico de los dos componentes JSX modificados; correcto.
- `node --check` de los módulos JS modificados; correcto.
- No fue posible ejecutar `mvnw test` aquí porque Maven Wrapper no pudo descargar Maven desde Maven Central. Por tanto, la compilación integral y las pruebas Spring/MongoDB deben ejecutarse en el equipo local.

## Comandos de verificación en Windows

Backend, desde la raíz del proyecto:

```powershell
.\mvnw.cmd test
```

Si PowerShell bloquea el wrapper, usar:

```powershell
cmd /c mvnw.cmd test
```

Frontend:

```powershell
cd ziplocSAS_Front
npm install
npm run build
npm run dev
```

La conexión a MongoDB debe estar activa antes de iniciar Spring Boot.
