package proyectocompiladores2.sintactico.arbol;

/**
 * Sentencia simple terminada en ';' (asignación, llamada a función...).
 * Sus tokens son los de la sentencia, sin el ';'.
 * Regla: simple → token { token } ';'
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoSentencia extends Nodo {

    public NodoSentencia(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Sentencia";
    }
}
