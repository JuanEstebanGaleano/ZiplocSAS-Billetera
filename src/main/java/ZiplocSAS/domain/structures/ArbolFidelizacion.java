package ZiplocSAS.domain.structures;

import ZiplocSAS.domain.model.Usuario;
import java.util.*;

/**
 * Arbol AVL de fidelizacion ordenado por puntos y, como desempate, por ID.
 * El indice hash auxiliar permite localizar un usuario por ID en O(1) promedio,
 * mientras el AVL conserva consultas ordenadas por rango en O(log n + k).
 */
public class ArbolFidelizacion {
    private Node root;
    private int size;
    private final Map<String, Node> indicePorId = new HashMap<>();

    private static class Node {
        Usuario usuario;
        int puntosClave;
        String idClave;
        Node left;
        Node right;
        int height = 1;

        Node(Usuario usuario) {
            this.usuario = usuario;
            this.puntosClave = usuario.getPuntosAcumulados();
            this.idClave = usuario.getId();
        }
    }

    public synchronized void insertar(Usuario usuario) {
        validarUsuario(usuario);
        Node anterior = indicePorId.remove(usuario.getId());
        if (anterior != null) {
            root = eliminar(root, anterior.puntosClave, anterior.idClave);
            size--;
        }
        Node nuevo = new Node(usuario);
        root = insertar(root, nuevo);
        indicePorId.put(usuario.getId(), nuevo);
        size++;
        imprimir("insertar/actualizar usuario " + usuario.getId());
    }

    public synchronized void actualizar(Usuario usuario) {
        insertar(usuario);
    }

    public synchronized Usuario buscar(String usuarioId) {
        Node nodo = indicePorId.get(usuarioId);
        return nodo == null ? null : nodo.usuario;
    }

    public synchronized boolean eliminar(String usuarioId) {
        Node nodo = indicePorId.remove(usuarioId);
        if (nodo == null) return false;
        root = eliminar(root, nodo.puntosClave, nodo.idClave);
        size--;
        imprimir("eliminar usuario " + usuarioId);
        return true;
    }

    public synchronized List<Usuario> buscarPorRango(int minPuntos, int maxPuntos) {
        List<Usuario> resultado = new ArrayList<>();
        if (minPuntos <= maxPuntos) buscarPorRango(root, minPuntos, maxPuntos, resultado);
        return resultado;
    }

    public synchronized List<Usuario> obtenerTop(int n) {
        if (n <= 0) return new ArrayList<>();
        List<Usuario> todos = new ArrayList<>();
        recorridoDescendente(root, todos, n);
        return todos;
    }

    public synchronized List<Usuario> recorridoInOrder() {
        List<Usuario> resultado = new ArrayList<>();
        inOrder(root, resultado);
        return resultado;
    }

    public synchronized int size() { return size; }
    public synchronized int altura() { return altura(root); }

    private Node insertar(Node actual, Node nuevo) {
        if (actual == null) return nuevo;
        if (comparar(nuevo.puntosClave, nuevo.idClave, actual) < 0)
            actual.left = insertar(actual.left, nuevo);
        else
            actual.right = insertar(actual.right, nuevo);
        return balancear(actual);
    }

    private Node eliminar(Node actual, int puntos, String id) {
        if (actual == null) return null;
        int cmp = comparar(puntos, id, actual);
        if (cmp < 0) actual.left = eliminar(actual.left, puntos, id);
        else if (cmp > 0) actual.right = eliminar(actual.right, puntos, id);
        else {
            if (actual.left == null) return actual.right;
            if (actual.right == null) return actual.left;
            Node sucesor = minimo(actual.right);
            actual.usuario = sucesor.usuario;
            actual.puntosClave = sucesor.puntosClave;
            actual.idClave = sucesor.idClave;
            indicePorId.put(sucesor.idClave, actual);
            actual.right = eliminar(actual.right, sucesor.puntosClave, sucesor.idClave);
        }
        return balancear(actual);
    }

    private Node balancear(Node nodo) {
        if (nodo == null) return null;
        actualizarAltura(nodo);
        int balance = factorBalance(nodo);
        if (balance > 1) {
            if (factorBalance(nodo.left) < 0) nodo.left = rotarIzquierda(nodo.left);
            return rotarDerecha(nodo);
        }
        if (balance < -1) {
            if (factorBalance(nodo.right) > 0) nodo.right = rotarDerecha(nodo.right);
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    private Node rotarDerecha(Node y) {
        Node x = y.left;
        Node t2 = x.right;
        x.right = y;
        y.left = t2;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private Node rotarIzquierda(Node x) {
        Node y = x.right;
        Node t2 = y.left;
        y.left = x;
        x.right = t2;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    private void actualizarAltura(Node n) { n.height = 1 + Math.max(altura(n.left), altura(n.right)); }
    private int altura(Node n) { return n == null ? 0 : n.height; }
    private int factorBalance(Node n) { return n == null ? 0 : altura(n.left) - altura(n.right); }
    private Node minimo(Node n) { while (n.left != null) n = n.left; return n; }

    private int comparar(int puntos, String id, Node nodo) {
        int cmp = Integer.compare(puntos, nodo.puntosClave);
        return cmp != 0 ? cmp : id.compareTo(nodo.idClave);
    }

    private void buscarPorRango(Node n, int min, int max, List<Usuario> salida) {
        if (n == null) return;
        if (n.puntosClave >= min) buscarPorRango(n.left, min, max, salida);
        if (n.puntosClave >= min && n.puntosClave <= max) salida.add(n.usuario);
        if (n.puntosClave <= max) buscarPorRango(n.right, min, max, salida);
    }

    private void inOrder(Node n, List<Usuario> salida) {
        if (n == null) return;
        inOrder(n.left, salida);
        salida.add(n.usuario);
        inOrder(n.right, salida);
    }

    private void recorridoDescendente(Node n, List<Usuario> salida, int limite) {
        if (n == null || salida.size() >= limite) return;
        recorridoDescendente(n.right, salida, limite);
        if (salida.size() < limite) salida.add(n.usuario);
        recorridoDescendente(n.left, salida, limite);
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getId().isBlank())
            throw new IllegalArgumentException("El usuario y su identificador son obligatorios");
    }

    private void imprimir(String accion) {
        EstructuraConsolePrinter.imprimir("ArbolFidelizacion", accion,
                "Tipo: Arbol AVL ordenado por puntos e ID" + System.lineSeparator()
                        + "Tamano: " + size + System.lineSeparator()
                        + "Altura raiz: " + altura(root) + System.lineSeparator());
    }
}
