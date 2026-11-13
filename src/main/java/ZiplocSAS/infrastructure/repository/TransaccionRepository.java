package ZiplocSAS.infrastructure.repository;

import ZiplocSAS.domain.model.Transaccion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
/**
 * Repositorio MongoDB para transacciones.
 */

public interface TransaccionRepository extends MongoRepository<Transaccion, String> {
    /**
     * Busca movimientos donde una billetera participa como origen o destino.
     *
     * @param billeteraOrigenId id usado para comparar contra origen.
     * @param billeteraDestinoId id usado para comparar contra destino.
     * @return transacciones relacionadas.
     */
    List<Transaccion> findByBilleteraOrigenIdOrBilleteraDestinoId(String billeteraOrigenId, String billeteraDestinoId);

    Optional<Transaccion> findByOperacionProgramadaId(String operacionProgramadaId);
}
