package proyectocompiladores2.sintactico.arbol;

/**
 * Condición de un while o un if: los tokens que hay entre los paréntesis.
 * Para este proyecto la condición se asume correcta; solo se valida que exista.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoCondicion extends Nodo {

    public NodoCondicion(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Condición";
    }
}
