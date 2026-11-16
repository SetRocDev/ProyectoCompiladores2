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

    private static final Comparator<ErrorLexico> POR_COLUMNA =
            Comparator.comparingInt(ErrorLexico::getColumna);

    private final Map<Integer, List<ErrorLexico>> erroresPorLinea = new TreeMap<>();

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
        List<ErrorLexico> lista = erroresPorLinea.computeIfAbsent(linea, k -> new ArrayList<>());
        for (ErrorLexico existente : lista) {
            if (existente.getCodigo() == codigo && existente.getColumna() == columna) {
                return;
            }
        }
        lista.add(new ErrorLexico(codigo, linea, columna, codigo.describir(datos)));
    }

    /**
     * @param linea número de línea (desde 1)
     * @return errores de esa línea ordenados por columna (lista vacía si no hay)
     */
    public List<ErrorLexico> erroresDeLinea(int linea) {
        List<ErrorLexico> lista = erroresPorLinea.get(linea);
        if (lista == null) {
            return Collections.emptyList();
        }
        List<ErrorLexico> ordenada = new ArrayList<>(lista);
        ordenada.sort(POR_COLUMNA); // sort estable: a igual columna respeta el orden de registro
        return Collections.unmodifiableList(ordenada);
    }

    /** @return total de errores del archivo. */
    public int total() {
        int total = 0;
        for (List<ErrorLexico> lista : erroresPorLinea.values()) {
            total += lista.size();
        }
        return total;
    }
}
