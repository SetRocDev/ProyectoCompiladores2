package proyectocompiladores2.sintactico.arbol;

/**
 * Estructura while: hijos = condición y bloque.
 * Regla: while → 'while' '(' condicion ')' bloque
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoWhile extends Nodo {

    public NodoWhile(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "While";
    }
}
