package proyectocompiladores2.sintactico.arbol;

/**
 * Estructura if: hijos = condición, bloque del if y, si existe, el else.
 * Regla: if → 'if' '(' condicion ')' bloque [ 'else' bloque ]
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoIf extends Nodo {

    public NodoIf(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "If";
    }
}
