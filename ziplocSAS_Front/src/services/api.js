import apiClient from '../api/axios.js';
import { parseApiError } from '../api/errorParser';

/* =====================================================
   PROCESAMIENTO DE RESPUESTAS
===================================================== */

function unwrap(response) {
  return response?.data ?? response ?? null;
}

/* =====================================================
   SOLICITUDES HTTP
===================================================== */

async function safeRequest(method, path, data, config = {}) {
  try {
    const response = await apiClient.request({
      method,
      url: path,
      data,
      ...config,
    });

    return unwrap(response);
  } catch (err) {
    const parsed = parseApiError(err);

    const error = new Error(parsed.message);
    error.status = parsed.status;
    error.data = parsed.data;
    error.isConnectionError = parsed.isConnectionError;

    throw error;
  }
}

/* =====================================================
   USUARIOS
===================================================== */

export function crearUsuario(payload) {
  return safeRequest('post', '/usuarios', payload);
}

export function obtenerUsuarios(config) {
  return safeRequest('get', '/usuarios', undefined, config);
}

export function obtenerUsuariosTopPuntos(config) {
  return safeRequest('get', '/usuarios/top-puntos', undefined, config);
}

export function obtenerUsuariosPorRangoPuntos(min, max, config = {}) {
  return safeRequest('get', '/usuarios/puntos/rango', undefined, {
    ...config,
    params: {
      min,
      max,
      ...(config.params || {}),
    },
  });
}

export function obtenerUsuarioPorId(id, config) {
  return safeRequest('get', `/usuarios/${id}`, undefined, config);
}

export function actualizarUsuario(id, payload) {
  return safeRequest('put', `/usuarios/${id}`, payload);
}

export function eliminarUsuario(id) {
  return safeRequest('delete', `/usuarios/${id}`);
}

export function toggleActivoUsuario(id, activo) {
  return safeRequest('patch', `/usuarios/${id}/activo`, undefined, {
    params: { activo },
  });
}

/* =====================================================
   BILLETERAS
===================================================== */

export function crearBilletera(payload) {
  return safeRequest('post', '/billeteras', payload);
}

export function obtenerBilleteras(config) {
  return safeRequest('get', '/billeteras', undefined, config);
}

export function obtenerBilleteraPorId(id, config) {
  return safeRequest('get', `/billeteras/${id}`, undefined, config);
}

export function obtenerBilleterasPorUsuario(usuarioId, config) {
  return safeRequest(
      'get',
      `/billeteras/usuario/${usuarioId}`,
      undefined,
      config
  );
}

export function actualizarBilletera(id, payload) {
  return safeRequest('put', `/billeteras/${id}`, payload);
}

export function eliminarBilletera(id) {
  return safeRequest('delete', `/billeteras/${id}`);
}

export function obtenerSaldoBilletera(id, config) {
  return safeRequest('get', `/billeteras/${id}/saldo`, undefined, config);
}

/* =====================================================
   TRANSACCIONES
===================================================== */

export function crearTransaccion(payload) {
  return safeRequest('post', '/transacciones', payload);
}

export function obtenerTransaccionPorId(id, config) {
  return safeRequest('get', `/transacciones/${id}`, undefined, config);
}

export function obtenerTransaccionesPorBilletera(billeteraId, config) {
  return safeRequest(
      'get',
      `/transacciones/billetera/${billeteraId}`,
      undefined,
      config
  );
}

export function recargarTransaccion(payload) {
  return safeRequest('post', '/transacciones/recargar', payload);
}

export function retirarTransaccion(payload) {
  return safeRequest('post', '/transacciones/retirar', payload);
}

export function transferirTransaccion(payload) {
  return safeRequest('post', '/transacciones/transferir', payload);
}

export function revertirTransaccion(id) {
  return safeRequest('post', `/transacciones/${id}/revertir`);
}

export function revertirUltimaTransaccion(payload) {
  return safeRequest('post', '/transacciones/revertir-ultima', payload);
}

export function verificarIntegridadTransacciones(config) {
  return safeRequest(
      'get',
      '/transacciones/integridad',
      undefined,
      config
  );
}

/* =====================================================
   RECOMPENSAS
===================================================== */

export function obtenerPuntosRecompensas(usuarioId, config) {
  return safeRequest(
      'get',
      `/recompensas/${usuarioId}/puntos`,
      undefined,
      config
  );
}

export function obtenerNivelRecompensas(usuarioId, config) {
  return safeRequest(
      'get',
      `/recompensas/${usuarioId}/nivel`,
      undefined,
      config
  );
}

export function canjearBeneficio(usuarioId, beneficioId) {
  return safeRequest(
      'post',
      `/recompensas/${usuarioId}/canjear/${beneficioId}`
  );
}

export function obtenerBeneficios(config) {
  return safeRequest('get', '/recompensas/beneficios', undefined, config);
}

/* =====================================================
   ANALÍTICA
===================================================== */

export function obtenerReporteAnalitica(usuarioId, config) {
  return safeRequest(
      'get',
      `/analitica/reporte/${usuarioId}`,
      undefined,
      config
  );
}

export function obtenerFrecuenciaTiposAnalitica(config) {
  return safeRequest(
      'get',
      '/analitica/frecuencia-tipos',
      undefined,
      config
  );
}

export function obtenerCategoriasActivasAnalitica(config) {
  return safeRequest(
      'get',
      '/analitica/categorias-activas',
      undefined,
      config
  );
}

export function obtenerMontoTotalAnalitica(config) {
  return safeRequest(
      'get',
      '/analitica/monto-total',
      undefined,
      config
  );
}

export function obtenerGrafoAnalitica(usuarioId, config) {
  return safeRequest(
      'get',
      `/analitica/grafo/${usuarioId}`,
      undefined,
      config
  );
}

export function obtenerGrafoBilleteras(config) {
  return safeRequest(
      'get',
      '/analitica/grafo-billeteras',
      undefined,
      config
  );
}

export function obtenerTransaccionesMayorValorAnalitica(config) {
  return safeRequest(
      'get',
      '/analitica/transacciones-mayor-valor',
      undefined,
      config
  );
}

export function obtenerRendimientoAnalitica(usuarioId, config) {
  return safeRequest(
      'get',
      `/analitica/rendimiento/${usuarioId}`,
      undefined,
      config
  );
}

export function obtenerTopBilleteras(config) {
  return safeRequest(
      'get',
      '/analitica/top-billeteras',
      undefined,
      config
  );
}

export function obtenerUsuariosActivos(config) {
  return safeRequest(
      'get',
      '/analitica/usuarios-activos',
      undefined,
      config
  );
}

export function obtenerAuditoriaUsuario(usuarioId, config) {
  return safeRequest(
      'get',
      `/analitica/auditoria/${usuarioId}`,
      undefined,
      config
  );
}

export function obtenerPatronesGasto(config) {
  return safeRequest(
      'get',
      '/analitica/patrones-gasto',
      undefined,
      config
  );
}

/* =====================================================
   OPERACIONES PROGRAMADAS
===================================================== */

export function obtenerOperacionesProgramadas(usuarioId, config = {}) {
  return safeRequest('get', '/programadas', undefined, {
    ...config,
    params: {
      ...(config.params || {}),
      usuarioId,
    },
  });
}

export function obtenerOperacionesProcesadas(usuarioId, config = {}) {
  return safeRequest('get', '/programadas/procesadas', undefined, {
    ...config,
    params: {
      ...(config.params || {}),
      usuarioId,
    },
  });
}

export function crearOperacionProgramadaApi(data) {
  return safeRequest('post', '/programadas', data);
}

export function procesarOperacionApi(hasta, usuarioId) {
  const params = {};

  if (hasta) {
    params.hasta = hasta;
  }

  if (usuarioId) {
    params.usuarioId = usuarioId;
  }

  return safeRequest('post', '/programadas/procesar', undefined, {
    params,
  });
}