package proyectocompiladores2.sintactico.arbol;

/**
 * Estructura for: hijos = cabecera (inicio; condición; incremento) y bloque.
 * Regla: for → 'for' '(' inicio ';' condicion ';' incremento ')' bloque
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoFor extends Nodo {

    public NodoFor(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "For";
    }
}
