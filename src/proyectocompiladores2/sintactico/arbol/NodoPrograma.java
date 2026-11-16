package proyectocompiladores2.sintactico.arbol;

/**
 * Raíz del árbol: todas las sentencias del archivo, entre <?php y ?>.
 * Regla: programa → '<?php' sentencias '?>'
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class NodoPrograma extends Nodo {

    public NodoPrograma(int linea) {
        super(linea);
    }

    @Override
    public String getNombre() {
        return "Programa";
    }
}
