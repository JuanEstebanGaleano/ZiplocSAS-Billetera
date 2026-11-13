package ZiplocSAS.infrastructure.persistence;

import ZiplocSAS.application.service.PlataformaContext;
import ZiplocSAS.domain.enums.EstadoTransaccion;
import ZiplocSAS.domain.model.Alerta;
import ZiplocSAS.domain.model.Billetera;
import ZiplocSAS.domain.model.EventoAuditoria;
import ZiplocSAS.domain.model.OperacionProgramada;
import ZiplocSAS.domain.model.Transaccion;
import ZiplocSAS.domain.model.Usuario;
import ZiplocSAS.infrastructure.repository.AlertaRepository;
import ZiplocSAS.infrastructure.repository.BilleteraRepository;
import ZiplocSAS.infrastructure.repository.EventoAuditoriaRepository;
import ZiplocSAS.infrastructure.repository.OperacionProgramadaRepository;
import ZiplocSAS.infrastructure.repository.TransaccionRepository;
import ZiplocSAS.infrastructure.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Carga datos persistidos en MongoDB hacia las estructuras en memoria
 * al iniciar la aplicación.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.data-loader",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MongoDataLoader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MongoDataLoader.class);

    private final PlataformaContext context;

    private final UsuarioRepository usuarioRepository;
    private final BilleteraRepository billeteraRepository;
    private final TransaccionRepository transaccionRepository;
    private final AlertaRepository alertaRepository;
    private final OperacionProgramadaRepository operacionProgramadaRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    /**
     * Sincroniza las colecciones persistidas con las estructuras
     * de datos en memoria.
     */
    @PostConstruct
    public void cargarDatosPersistidos() {

        ejecutarCarga("usuarios", this::cargarUsuarios);

        ejecutarCarga("billeteras", this::cargarBilleteras);

        ejecutarCarga("transacciones", this::cargarTransacciones);

        ejecutarCarga("alertas", this::cargarAlertas);

        ejecutarCarga(
                "operaciones programadas",
                this::cargarOperacionesProgramadas
        );

        ejecutarCarga(
                "eventos de auditoria",
                this::cargarEventosAuditoria
        );
    }

    /**
     * Ejecuta una carga de forma independiente.
     * Un error en una colección no detiene las demás.
     */
    private void ejecutarCarga(
            String nombre,
            Runnable carga
    ) {

        try {

            carga.run();

            LOGGER.info(
                    "Carga de {} completada correctamente",
                    nombre
            );

        } catch (RuntimeException ex) {

            LOGGER.error(
                    "Error cargando {}: {}",
                    nombre,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Carga usuarios en la tabla hash, árbol y grafo.
     */
    private void cargarUsuarios() {

        for (Usuario usuario : usuarioRepository.findAll()) {

            context.getTablaUsuarios().insertar(usuario);

            context.getArbolFidelizacion().insertar(usuario);

            context.getGrafoTransferencias()
                    .agregarVertice(usuario.getId());
        }
    }

    /**
     * Carga las billeteras en la tabla correspondiente.
     */
    private void cargarBilleteras() {

        billeteraRepository.findAll()
                .forEach(context.getTablaBilleteras()::insertar);
    }

    /**
     * Carga las transacciones ordenadas cronológicamente.
     */
    private void cargarTransacciones() {

        transaccionRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Transaccion::getFecha,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .forEach(transaccion -> {

                    context.getGestorTransacciones()
                            .agregar(transaccion);

                    registrarTransferenciaEnGrafo(transaccion);
                });
    }

    /**
     * Carga las alertas en las colas de notificaciones.
     */
    private void cargarAlertas() {

        alertaRepository.findAll()
                .stream()
                .filter(alerta -> alerta.getUsuarioId() != null)
                .sorted(
                        Comparator.comparing(
                                Alerta::getFecha,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .forEach(alerta ->

                        context.colaNotificaciones(
                                alerta.getUsuarioId()
                        ).encolar(alerta)
                );
    }

    /**
     * Reconstruye la cola de operaciones programadas.
     */
    private void cargarOperacionesProgramadas() {

        List<OperacionProgramada> pendientes =
                operacionProgramadaRepository
                        .findByEjecutadaFalseOrderByFechaEjecucionAscPrioridadAsc();

        context.getColaProgramadas().limpiar();

        pendientes.forEach(
                context.getColaProgramadas()::encolar
        );

        context.getOperacionesProcesadas().clear();

        context.getOperacionesProcesadas().addAll(
                operacionProgramadaRepository
                        .findByEjecutadaTrueOrderByFechaEjecucionAscPrioridadAsc()
        );

        LOGGER.info(
                "Operaciones programadas cargadas: {} pendientes y {} procesadas",
                pendientes.size(),
                context.getOperacionesProcesadas().size()
        );
    }

    /**
     * Carga los eventos de auditoría ordenados por fecha.
     */
    private void cargarEventosAuditoria() {

        eventoAuditoriaRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                EventoAuditoria::getFecha,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .forEach(
                        context.getEventosAuditoria()::add
                );
    }

    /**
     * Registra las transferencias completadas en los grafos.
     */
    private void registrarTransferenciaEnGrafo(
            Transaccion transaccion
    ) {

        if (transaccion.getEstado() != EstadoTransaccion.COMPLETADA
                || transaccion.getBilleteraOrigenId() == null
                || transaccion.getBilleteraDestinoId() == null) {

            return;
        }

        Billetera origen =
                context.getTablaBilleteras()
                        .buscarPorCodigo(
                                transaccion.getBilleteraOrigenId()
                        );

        Billetera destino =
                context.getTablaBilleteras()
                        .buscarPorCodigo(
                                transaccion.getBilleteraDestinoId()
                        );

        if (origen == null || destino == null) {
            return;
        }

        context.getGrafoTransferencias().agregarArista(
                origen.getUsuarioId(),
                destino.getUsuarioId(),
                transaccion.getValor()
        );

        context.getGrafoBilleteras().agregarArista(
                origen.getId(),
                destino.getId(),
                transaccion.getValor()
        );
    }
}