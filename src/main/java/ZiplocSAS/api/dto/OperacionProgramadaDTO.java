package ZiplocSAS.api.dto;

import ZiplocSAS.domain.enums.TipoTransaccion;
import ZiplocSAS.domain.enums.FrecuenciaRecurrencia;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for OperacionProgramada requests/responses
 */
/**
 * DTO de operaciones programadas para agenda y consulta de pagos diferidos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperacionProgramadaDTO {
    private String id;
    private LocalDateTime fechaEjecucion;
    private TipoTransaccion tipo;
    private Double valor;
    private String billeteraOrigenId;
    private String billeteraDestinoId;
    private Boolean ejecutada;
    private Integer prioridad;
    private String ultimoError;
    private FrecuenciaRecurrencia frecuencia;
    private String usuarioId;
    private String categoriaGasto;
}

