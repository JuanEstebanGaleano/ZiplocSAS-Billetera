import axios from 'axios';
import { clearAuthSession, getAccessToken, decodeJwtPayload } from '../auth/authStorage';

const API_BASE =
    import.meta.env.VITE_API_URL ||
    import.meta.env.REACT_APP_API_URL ||
    'http://localhost:8080/api';

const DEMO_USER_ID = 1;

const DEMO_USER = {
  id: DEMO_USER_ID,
  nombre: 'Usuario Demo',
  email: 'demo@ZiplocSAS.local',
  telefono: '3000000000',
  puntos: 1200,
  nivel: 'Oro',
  creadoAt: '2026-05-15T00:00:00.000Z',
  actualizadoAt: '2026-05-15T00:00:00.000Z',
  activo: true,
  estado: 'ACTIVO',
};

const DEMO_BILLETERAS = [
  {
    id: 1,
    usuarioId: DEMO_USER_ID,
    nombre: 'Billetera principal',
    tipo: 'Ahorro',
    saldo: 245000,
    estado: 'activo',
  },
  {
    id: 2,
    usuarioId: DEMO_USER_ID,
    nombre: 'Gastos diarios',
    tipo: 'Gastos diarios',
    saldo: 78000,
    estado: 'activo',
  },
];

const DEMO_TRANSACCIONES = {
  1: [
    {
      id: 101,
      tipo: 'INGRESO',
      monto: 120000,
      descripcion: 'Abono de nómina',
      fecha: '2026-05-14T16:20:00.000Z',
    },
    {
      id: 102,
      tipo: 'EGRESO',
      monto: 35000,
      descripcion: 'Pago de servicios',
      fecha: '2026-05-14T18:40:00.000Z',
    },
    {
      id: 103,
      tipo: 'TRANSFERENCIA',
      monto: 18000,
      descripcion: 'Transferencia a contacto',
      fecha: '2026-05-15T08:10:00.000Z',
    },
  ],
  2: [
    {
      id: 201,
      tipo: 'EGRESO',
      monto: 15000,
      descripcion: 'Mercado',
      fecha: '2026-05-15T09:30:00.000Z',
    },
    {
      id: 202,
      tipo: 'EGRESO',
      monto: 8200,
      descripcion: 'Transporte',
      fecha: '2026-05-15T11:05:00.000Z',
    },
  ],
};

const DEMO_NOTIFICACIONES = [
  {
    id: 1,
    usuarioId: DEMO_USER_ID,
    titulo: 'Bienvenido a ZiplocSAS',
    mensaje: 'Tu cuenta ha sido activada exitosamente.',
    tipo: 'bienvenida',
    leida: false,
    fecha: '2026-05-15T08:00:00.000Z',
  },
  {
    id: 2,
    usuarioId: DEMO_USER_ID,
    titulo: 'Pago recibido',
    mensaje: 'Se acreditaron $120.000 en tu billetera principal.',
    tipo: 'pago',
    leida: false,
    fecha: '2026-05-14T16:21:00.000Z',
  },
  {
    id: 3,
    usuarioId: DEMO_USER_ID,
    titulo: 'Nivel Oro alcanzado',
    mensaje: 'Felicitaciones, alcanzaste el nivel Oro.',
    tipo: 'recompensa',
    leida: true,
    fecha: '2026-05-13T10:00:00.000Z',
  },
];

/* =====================================================
   SESIÓN DEMO
===================================================== */

function isDemoSession() {
  const token = getAccessToken();

  if (!token) return false;

  const payload = decodeJwtPayload(token);

  return Boolean(payload?.demo);
}

/* =====================================================
   DATOS DE DEMOSTRACIÓN
===================================================== */

function buildDemoFallback(path) {
  if (/^\/usuarios\/\d+$/.test(path)) {
    return DEMO_USER;
  }

  if (
      /^\/usuarios$/.test(path) ||
      /^\/usuarios\/top-puntos$/.test(path) ||
      /^\/usuarios\/puntos\/rango$/.test(path)
  ) {
    return [DEMO_USER];
  }

  if (/^\/billeteras\/usuario\/\d+$/.test(path)) {
    return {
      billeteras: DEMO_BILLETERAS,
      total: DEMO_BILLETERAS.length,
    };
  }

  if (/^\/billeteras\/\d+\/saldo$/.test(path)) {
    const id = Number(path.split('/')[2]);

    const wallet =
        DEMO_BILLETERAS.find((item) => item.id === id) ||
        DEMO_BILLETERAS[0];

    return {
      billeteraId: wallet.id,
      saldo: wallet.saldo,
    };
  }

  if (/^\/billeteras\/\d+$/.test(path)) {
    const id = Number(path.split('/')[2]);

    return (
        DEMO_BILLETERAS.find((wallet) => wallet.id === id) ||
        DEMO_BILLETERAS[0]
    );
  }

  if (/^\/transacciones\/billetera\/\d+$/.test(path)) {
    const id = Number(path.split('/')[3]);

    return {
      transacciones: DEMO_TRANSACCIONES[id] || [],
    };
  }

  if (/^\/transacciones\/\d+$/.test(path)) {
    const allTransactions = Object.values(DEMO_TRANSACCIONES).flat();
    const id = Number(path.split('/')[2]);

    return allTransactions.find((item) => item.id === id) || null;
  }

  if (/^\/recompensas\/\d+\/puntos$/.test(path)) {
    return {
      usuarioId: DEMO_USER_ID,
      puntos: DEMO_USER.puntos,
      puntosAcumulados: DEMO_USER.puntos,
      nivel: DEMO_USER.nivel,
    };
  }

  if (/^\/recompensas\/\d+\/nivel$/.test(path)) {
    return {
      usuarioId: DEMO_USER_ID,
      nivel: DEMO_USER.nivel,
      puntosActuales: DEMO_USER.puntos,
      umbralSiguiente: 1500,
    };
  }

  if (/^\/recompensas\/beneficios$/.test(path)) {
    return {
      beneficios: [
        {
          id: '1',
          descripcion: 'Reduce el costo de operaciones frecuentes.',
          nivelRequerido: 'Bronce',
          puntosNecesarios: 120,
          tipo: 'descuento_transferencias',
          activo: true,
        },
        {
          id: '2',
          descripcion: 'Recibe bonificación directa sobre tu billetera.',
          nivelRequerido: 'Plata',
          puntosNecesarios: 180,
          tipo: 'bonificacion_saldo',
          activo: true,
        },
      ],
    };
  }

  if (/^\/notificaciones\/\d+$/.test(path)) {
    return {
      notificaciones: DEMO_NOTIFICACIONES,
    };
  }

  if (/^\/notificaciones\/\d+\/\d+\/leer$/.test(path)) {
    return { ok: true };
  }

  if (/^\/notificaciones\/\d+\/despachar$/.test(path)) {
    return { ok: true };
  }

  if (/^\/analitica\//.test(path)) {
    return {
      data: [],
      total: 0,
    };
  }

  return null;
}

/* =====================================================
   CONFIGURACIÓN AXIOS
===================================================== */

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

/* =====================================================
   INTERCEPTOR DE SOLICITUD
===================================================== */

apiClient.interceptors.request.use(
    (config) => {
      if (config.skipAuthHeader) return config;

      const token = getAccessToken();

      if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error) => Promise.reject(error)
);

/* =====================================================
   INTERCEPTOR DE RESPUESTA
===================================================== */

apiClient.interceptors.response.use(
    (response) => {
      const payload = response.data;

      // Respuesta estándar ApiResponse<T>
      if (
          payload &&
          typeof payload === 'object' &&
          !Array.isArray(payload) &&
          'success' in payload &&
          'data' in payload
      ) {
        if (!payload.success) {
          const error = new Error(
              payload.message || 'Error en la respuesta del servidor'
          );

          error.status = response.status;
          error.data = payload;

          throw error;
        }

        return {
          ...response,
          data: payload.data,
        };
      }

      // Respuestas directas: listas, DTO y objetos.
      return response;
    },
    async (error) => {
      // Fallback de demostración únicamente para errores 404.
      if (error.response?.status === 404 && isDemoSession()) {
        const fallback = buildDemoFallback(error.config?.url || '');

        if (fallback !== null) {
          return {
            data: fallback,
            status: 200,
            statusText: 'OK - DEMO',
            headers: {},
            config: error.config,
          };
        }
      }

      const originalRequest = error.config || {};

      // Sesión inválida o expirada.
      if (
          error.response?.status === 401 &&
          !originalRequest.skipAuthRefresh
      ) {
        clearAuthSession();

        const authError = new Error('Sesión inválida o expirada');

        authError.status = 401;
        authError.isAuthError = true;

        throw authError;
      }

      // Problemas de conexión.
      if (
          error.code === 'ECONNREFUSED' ||
          error.code === 'ERR_NETWORK' ||
          !error.response
      ) {
        const connectionError = new Error(
            `Backend server is unavailable. Please check if the server is running at ${API_BASE}`
        );

        connectionError.status = 0;
        connectionError.isConnectionError = true;

        throw connectionError;
      }

      // Errores HTTP.
      const serverMessage =
          error.response?.data?.error ||
          error.response?.data?.detail ||
          error.response?.data?.message ||
          error.message ||
          'Network error';

      const serverError = new Error(serverMessage);

      serverError.status = error.response.status;
      serverError.data = error.response.data;

      throw serverError;
    }
);

export { API_BASE };

export default apiClient;