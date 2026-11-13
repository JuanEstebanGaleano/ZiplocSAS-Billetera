package ZiplocSAS.domain.structures;

import ZiplocSAS.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArbolFidelizacionTest {
    @Test
    void mantieneBalanceAvlConInsercionesOrdenadas() {
        ArbolFidelizacion arbol = new ArbolFidelizacion();
        for (int i = 1; i <= 50; i++) {
            arbol.insertar(usuario("u" + i, i));
        }
        assertEquals(50, arbol.size());
        assertTrue(arbol.altura() < 10, "La altura debe permanecer logaritmica");
        assertEquals(50, arbol.buscarPorRango(1, 50).size());
    }

    @Test
    void admitePuntosDuplicadosYActualizaSinDuplicar() {
        ArbolFidelizacion arbol = new ArbolFidelizacion();
        Usuario ana = usuario("ana", 500);
        Usuario luis = usuario("luis", 500);
        arbol.insertar(ana);
        arbol.insertar(luis);
        ana.setPuntosAcumulados(900);
        arbol.actualizar(ana);

        assertEquals(2, arbol.size());
        assertEquals(ana, arbol.buscar("ana"));
        assertEquals(1, arbol.buscarPorRango(900, 900).size());
        assertTrue(arbol.eliminar("luis"));
        assertEquals(1, arbol.size());
    }

    private Usuario usuario(String id, int puntos) {
        return Usuario.builder().id(id).nombre(id).puntosAcumulados(puntos).build();
    }
}
