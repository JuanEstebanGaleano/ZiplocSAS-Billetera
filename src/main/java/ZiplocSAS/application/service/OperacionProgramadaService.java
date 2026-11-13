package ZiplocSAS.application.service;

import ZiplocSAS.domain.enums.TipoAlerta;
import ZiplocSAS.domain.enums.FrecuenciaRecurrencia;
import ZiplocSAS.domain.model.Billetera;
import ZiplocSAS.domain.model.OperacionProgramada;
import ZiplocSAS.domain.model.Transaccion;
import ZiplocSAS.infrastructure.repository.OperacionProgramadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * Servicio para administrar la cola de operaciones programadas.
 */

@Service
@RequiredArgsConstructor
public class OperacionProgramadaService {
    private final PlataformaContext context;
    private final TransaccionService transaccionService;
    private final BilleteraService billeteraService;
    private final NotificacionService notificacionService;
    private final OperacionProgramadaRepository operacionProgramadaRepository;

    /**
     * Agenda una operacion y la encola por fecha de ejecucion y prioridad.
     *
     * @param operacion datos de la operacion a programar.
     * @return operacion persistida.
     */
    public synchronized OperacionProgramada programar(OperacionProgramada operacion) {
        if (operacion == null) {
            throw new IllegalArgumentException("La operacion programada es obligatoria");
        }
        if (!Double.isFinite(operacion.getValor()) || operacion.getValor() <= 0) {
            throw new IllegalArgumentException("El valor programado debe ser positivo");
        }
        if (operacion.getFechaEjecucion() == null) {
            throw new IllegalArgumentException("La fecha de ejecucion es obligatoria");
        }
        if (operacion.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de operacion es obligatorio");
        }
        validarBilleteras(operacion);
        operacion.setId(UUID.randomUUID().toString());
        operacion.setEjecutada(false);
        operacion.setUltimoError(null);
        operacion.setUsuarioId(usuarioOperacion(operacion));
        if (operacion.getFrecuencia() == null) operacion.setFrecuencia(FrecuenciaRecurrencia.NINGUNA);

        OperacionProgramada guardada = operacionProgramadaRepository.save(operacion);
        context.getColaProgramadas().encolar(guardada);
        notificarProgramacion(guardada);
        return guardada;
    }

    public List<OperacionProgramada> listarPendientes(String usuarioId) {
        List<OperacionProgramada> pendientes = operacionProgramadaRepository
                .findByEjecutadaFalseOrderByFechaEjecucionAscPrioridadAsc();
        if (pendientes.isEmpty()) pendientes = context.getColaProgramadas().obtenerTodas();
        if (usuarioId == null || usuarioId.isBlank()) return pendientes;
        return pendientes.stream().filter(op -> usuarioId.equals(op.getUsuarioId())
                || usuarioId.equals(usuarioOperacion(op))).toList();
    }

    public List<OperacionProgramada> listarProcesadas(String usuarioId) {
        List<OperacionProgramada> procesadas = operacionProgramadaRepository
                .findByEjecutadaTrueOrderByFechaEjecucionAscPrioridadAsc();
        if (procesadas.isEmpty()) procesadas = context.getOperacionesProcesadas();
        if (usuarioId == null || usuarioId.isBlank()) return procesadas;
        return procesadas.stream().filter(op -> usuarioId.equals(op.getUsuarioId())
                || usuarioId.equals(usuarioOperacion(op))).toList();
    }

    /**
     * Reconstruye el heap desde MongoDB y ejecuta solo las operaciones vencidas.
     * Las fallidas quedan persistidas como pendientes para permitir un reintento
     * en una ejecucion posterior, sin bloquear el resto del lote.
     */
    public synchronized List<OperacionProgramada> procesarPendientes(LocalDateTime fechaCorte) {
        return procesarPendientes(fechaCorte, null);
    }

    public synchronized List<OperacionProgramada> procesarPendientes(LocalDateTime fechaCorte, String usuarioId) {
        LocalDateTime corte = fechaCorte == null ? LocalDateTime.now() : fechaCorte;
        List<OperacionProgramada> pendientes = operacionProgramadaRepository
                .findByEjecutadaFalseOrderByFechaEjecucionAscPrioridadAsc();
        if (usuarioId != null && !usuarioId.isBlank()) {
            pendientes = pendientes.stream()
                    .filter(op -> usuarioId.equals(op.getUsuarioId()) || usuarioId.equals(usuarioOperacion(op)))
                    .toList();
        }

        context.getColaProgramadas().limpiar();
        pendientes.forEach(context.getColaProgramadas()::encolar);

        List<OperacionProgramada> intentadas = new ArrayList<>();
        while (!context.getColaProgramadas().estaVacia()) {
            OperacionProgramada siguiente = context.getColaProgramadas().peek();
            if (siguiente == null || siguiente.getFechaEjecucion() == null
                    || siguiente.getFechaEjecucion().isAfter(corte)) {
                break;
            }

            OperacionProgramada operacion = context.getColaProgramadas().procesarSiguiente();
            try {
                transaccionService.crear(Transaccion.builder()
                        .tipo(operacion.getTipo())
                        .valor(operacion.getValor())
                        .billeteraOrigenId(operacion.getBilleteraOrigenId())
                        .billeteraDestinoId(operacion.getBilleteraDestinoId())
                        .operacionProgramadaId(operacion.getId())
                        .categoriaGasto(operacion.getCategoriaGasto())
                        .build());
                operacion.setEjecutada(true);
                operacion.setUltimoError(null);
            } catch (RuntimeException ex) {
                operacion.setEjecutada(false);
                operacion.setUltimoError(mensajeError(ex));
                notificarFallo(operacion);
            }
            operacionProgramadaRepository.save(operacion);
            if (operacion.isEjecutada()) {
                context.getOperacionesProcesadas().add(operacion);
                try {
                    programarSiguienteRecurrencia(operacion);
                } catch (RuntimeException recurrenciaEx) {
                    operacion.setUltimoError("Transaccion ejecutada; no se pudo crear la siguiente recurrencia: "
                            + mensajeError(recurrenciaEx));
                    operacionProgramadaRepository.save(operacion);
                }
            }
            intentadas.add(operacion);
        }
        return intentadas;
    }

    private void programarSiguienteRecurrencia(OperacionProgramada actual) {
        if (actual.getFrecuencia() == null || actual.getFrecuencia() == FrecuenciaRecurrencia.NINGUNA) return;
        LocalDateTime siguienteFecha = switch (actual.getFrecuencia()) {
            case DIARIA -> actual.getFechaEjecucion().plusDays(1);
            case SEMANAL -> actual.getFechaEjecucion().plusWeeks(1);
            case MENSUAL -> actual.getFechaEjecucion().plusMonths(1);
            case NINGUNA -> null;
        };
        if (siguienteFecha == null) return;
        OperacionProgramada siguiente = OperacionProgramada.builder()
                .id(UUID.randomUUID().toString())
                .fechaEjecucion(siguienteFecha)
                .tipo(actual.getTipo())
                .valor(actual.getValor())
                .billeteraOrigenId(actual.getBilleteraOrigenId())
                .billeteraDestinoId(actual.getBilleteraDestinoId())
                .prioridad(actual.getPrioridad())
                .frecuencia(actual.getFrecuencia())
                .usuarioId(actual.getUsuarioId())
                .categoriaGasto(actual.getCategoriaGasto())
                .ejecutada(false)
                .build();
        OperacionProgramada guardada = operacionProgramadaRepository.save(siguiente);
        context.getColaProgramadas().encolar(guardada);
    }

    private void validarBilleteras(OperacionProgramada operacion) {
        switch (operacion.getTipo()) {
            case RECARGA -> {
                if (operacion.getBilleteraDestinoId() == null || operacion.getBilleteraDestinoId().isBlank())
                    throw new IllegalArgumentException("La recarga requiere una billetera destino");
                billeteraService.obtenerObligatoria(operacion.getBilleteraDestinoId());
            }
            case RETIRO -> {
                if (operacion.getBilleteraOrigenId() == null || operacion.getBilleteraOrigenId().isBlank())
                    throw new IllegalArgumentException("El retiro requiere una billetera origen");
                billeteraService.obtenerObligatoria(operacion.getBilleteraOrigenId());
            }
            case TRANSFERENCIA, PAGO_PROGRAMADO -> {
                if (operacion.getBilleteraOrigenId() == null || operacion.getBilleteraOrigenId().isBlank()
                        || operacion.getBilleteraDestinoId() == null || operacion.getBilleteraDestinoId().isBlank())
                    throw new IllegalArgumentException("La transferencia requiere billetera origen y destino");
                if (operacion.getBilleteraOrigenId().equals(operacion.getBilleteraDestinoId()))
                    throw new IllegalArgumentException("Las billeteras origen y destino deben ser diferentes");
                billeteraService.obtenerObligatoria(operacion.getBilleteraOrigenId());
                billeteraService.obtenerObligatoria(operacion.getBilleteraDestinoId());
            }
        }
    }

    private String mensajeError(RuntimeException ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) return "No fue posible procesar la operacion";
        return mensaje.length() > 240 ? mensaje.substring(0, 240) : mensaje;
    }

    private void notificarProgramacion(OperacionProgramada operacion) {
        String usuarioId = usuarioOperacion(operacion);
        if (usuarioId != null) {
            notificacionService.crear(
                    usuarioId,
                    TipoAlerta.RECORDATORIO_PROGRAMADA,
                    "Recordatorio: tienes una operacion programada para " + operacion.getFechaEjecucion()
            );
        }
    }

    private void notificarFallo(OperacionProgramada operacion) {
        String usuarioId = usuarioOperacion(operacion);
        if (usuarioId != null) {
            notificacionService.crear(usuarioId, TipoAlerta.TRANSACCION_FALLIDA, "Operacion programada rechazada");
        }
    }

    private String usuarioOperacion(OperacionProgramada operacion) {
        String billeteraId = operacion.getBilleteraOrigenId() != null
                ? operacion.getBilleteraOrigenId()
                : operacion.getBilleteraDestinoId();
        if (billeteraId == null) {
            return null;
        }
        try {
            Billetera billetera = billeteraService.obtenerObligatoria(billeteraId);
            return billetera.getUsuarioId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
