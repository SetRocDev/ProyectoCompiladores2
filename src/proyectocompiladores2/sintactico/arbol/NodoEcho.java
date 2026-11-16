package proyectocompiladores2.sintactico.arbol;

/**
 * Sentencia echo. Sus tokens son los de la expresión que se imprime.
 * Regla: echo → 'echo' expresion { ',' expresion } ';'
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoEcho extends Nodo {

    public NodoEcho(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Echo";
    }
}
