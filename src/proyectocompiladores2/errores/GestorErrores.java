package proyectocompiladores2.errores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Acumula los errores encontrados durante el análisis de un archivo.
 *
 * Una misma línea puede tener varios errores distintos: se guardan todos y
 * se devuelven ordenados por columna, para escribirlos debajo de su línea
 * en el .log.
 *
 * Todo error pertenece a una línea (Opción 1 del enunciado, de forma
 * consistente en todo el archivo). Incluso los errores que se refieren al
 * archivo completo se asocian a la línea más lógica: si falta '<?php' va en
 * la línea 0001 y si falta '?>' en la última línea (corrección del
 * Proyecto 1, tarea T16; antes se escribían al final sin número de línea).
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class GestorErrores {

    private static final Comparator<ErrorCompilacion> POR_COLUMNA =
            Comparator.comparingInt(ErrorCompilacion::getColumna);

    private final Map<Integer, List<ErrorCompilacion>> erroresPorLinea = new TreeMap<>();

    /**
     * Registra un error en una línea concreta.
     * Si ya existe el mismo error (código, línea y columna) no se duplica,
     * por si dos validadores detectan el mismo problema.
     *
     * @param codigo  tipo de error
     * @param linea   número de línea (desde 1)
     * @param columna columna donde inicia el problema (desde 1)
     * @param datos   valores para completar la descripción
     */
    public void registrar(CodigoError codigo, int linea, int columna, Object... datos) {
        List<ErrorCompilacion> lista = erroresPorLinea.computeIfAbsent(linea, k -> new ArrayList<>());
        for (ErrorCompilacion existente : lista) {
            if (existente.getCodigo() == codigo && existente.getColumna() == columna) {
                return;
            }
        }
        lista.add(new ErrorCompilacion(codigo, linea, columna, codigo.describir(datos)));
    }

    /**
     * Quita un error ya registrado. Lo usa una etapa posterior cuando puede
     * describir el mismo problema de forma más específica (un error, un
     * mensaje). Por ejemplo, una cadena sin cerrar dentro de un echo se
     * reporta como Error 803/804 en lugar del 502 general.
     *
     * @return true si el error existía y se quitó
     */
    public boolean quitar(CodigoError codigo, int linea, int columna) {
        List<ErrorCompilacion> lista = erroresPorLinea.get(linea);
        if (lista == null) {
            return false;
        }
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getCodigo() == codigo && lista.get(i).getColumna() == columna) {
                lista.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @param linea número de línea (desde 1)
     * @return errores de esa línea ordenados por columna (lista vacía si no hay)
     */
    public List<ErrorCompilacion> erroresDeLinea(int linea) {
        List<ErrorCompilacion> lista = erroresPorLinea.get(linea);
        if (lista == null) {
            return Collections.emptyList();
        }
        List<ErrorCompilacion> ordenada = new ArrayList<>(lista);
        ordenada.sort(POR_COLUMNA); // sort estable: a igual columna respeta el orden de registro
        return Collections.unmodifiableList(ordenada);
    }

    /** @return cantidad de errores de una etapa ("Léxico", "Sintáctico" o "Semántico"). */
    public int total(String etapa) {
        int total = 0;
        for (List<ErrorCompilacion> lista : erroresPorLinea.values()) {
            for (ErrorCompilacion error : lista) {
                if (error.getCodigo().getEtapa().equals(etapa)) {
                    total++;
                }
            }
        }
        return total;
    }

    /** @return total de errores del archivo. */
    public int total() {
        int total = 0;
        for (List<ErrorCompilacion> lista : erroresPorLinea.values()) {
            total += lista.size();
        }
        return total;
    }
}
