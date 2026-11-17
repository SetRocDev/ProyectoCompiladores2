package proyectocompiladores2.semantico;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import proyectocompiladores2.lexico.Patrones;

/**
 * Tabla de símbolos del análisis semántico: cada variable que recibió un
 * valor y la línea donde lo recibió por primera vez.
 *
 * Distingue mayúsculas y minúsculas, igual que PHP y que el Proyecto 1:
 * $total y $Total son dos variables distintas.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class TablaSimbolos {

    /** Variables que PHP define solo (no hace falta asignarlas). */
    private static final Set<String> PREDEFINIDAS = Patrones.conjunto(
            "$this", "$GLOBALS", "$_SERVER", "$_GET", "$_POST", "$_FILES", "$_COOKIE",
            "$_SESSION", "$_REQUEST", "$_ENV", "$argv", "$argc");

    /** Largo mínimo del nombre (sin el $) para sugerir una variable que difiere en una letra. */
    private static final int LARGO_MINIMO_PARA_SUGERIR = 3;

    private final Map<String, Integer> asignadas = new LinkedHashMap<>();

    /** Registra que la variable recibió un valor en esa línea (se guarda la primera vez). */
    public void asignar(String variable, int linea) {
        asignadas.putIfAbsent(variable, linea);
    }

    /** @return true si la variable ya recibió un valor o es una variable predefinida de PHP. */
    public boolean estaAsignada(String variable) {
        return asignadas.containsKey(variable) || PREDEFINIDAS.contains(variable);
    }

    /**
     * @return la variable asignada que más se parece (por ejemplo $total si
     *         se escribió $Total), o null si ninguna se parece lo suficiente
     */
    public String parecida(String variable) {
        String mejor = null;
        int mejorDistancia = 2; // solo cambia en mayúsculas o en una letra
        for (String asignada : asignadas.keySet()) {
            int distancia = asignada.equalsIgnoreCase(variable) ? 0
                    : Patrones.distanciaDeEdicion(asignada, variable);
            // En nombres muy cortos una letra de diferencia no es una pista: $x y $i son variables distintas
            if (distancia == 1 && Math.min(asignada.length(), variable.length()) - 1 < LARGO_MINIMO_PARA_SUGERIR) {
                continue;
            }
            if (distancia < mejorDistancia) {
                mejor = asignada;
                mejorDistancia = distancia;
            }
        }
        return mejor;
    }

    /** @return las variables asignadas, en el orden en que aparecen. */
    public Map<String, Integer> getAsignadas() {
        return asignadas;
    }
}
