package proyectocompiladores2.io;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contenido de un archivo .php tal como se leyó del disco.
 *
 * Cada línea se guarda sin su salto de línea, pero se conserva todo lo demás
 * (espacios, tabulaciones, líneas en blanco). El salto de línea de CADA
 * línea se guarda por separado ("\r\n", "\n", "\r" o "" si la última línea
 * no tiene), porque un mismo archivo puede mezclar formatos y el .log debe
 * reproducirlos exactamente (R2; corrección del Proyecto 1 en la tarea T15).
 *
 * También se registra si el archivo tiene BOM UTF-8 (R4: nada antes de '<?php')
 * y con qué codificación se leyó, para escribir el .log con la misma (R2).
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public final class ArchivoFuente {

    /** Separador que se usa cuando el archivo no tiene ningún salto de línea. */
    private static final String SEPARADOR_WINDOWS = "\r\n";

    private final Path ruta;
    private final List<String> lineas;
    private final List<String> saltos;
    private final boolean tieneBom;
    private final Charset codificacion;

    /**
     * @param ruta         archivo leído
     * @param lineas       texto de cada línea, sin su salto de línea
     * @param saltos       salto de línea original de cada línea (misma cantidad que lineas)
     * @param tieneBom     true si el archivo inicia con la marca BOM de UTF-8
     * @param codificacion codificación con la que se leyó (UTF-8 o la de Windows)
     */
    public ArchivoFuente(Path ruta, List<String> lineas, List<String> saltos, boolean tieneBom,
            Charset codificacion) {
        if (lineas.size() != saltos.size()) {
            throw new IllegalArgumentException("Cada línea debe tener su salto de línea");
        }
        this.ruta = ruta;
        this.lineas = Collections.unmodifiableList(new ArrayList<>(lineas));
        this.saltos = Collections.unmodifiableList(new ArrayList<>(saltos));
        this.tieneBom = tieneBom;
        this.codificacion = codificacion;
    }

    /** @return ruta completa del archivo leído. */
    public Path getRuta() {
        return ruta;
    }

    /** @return nombre del archivo sin la extensión .php (por ejemplo "CodigoProyecto"). */
    public String getNombreBase() {
        String nombre = ruta.getFileName().toString();
        return nombre.substring(0, nombre.length() - LocalizadorArchivo.EXTENSION.length());
    }

    /** @return cantidad de líneas. */
    public int getCantidadLineas() {
        return lineas.size();
    }

    /**
     * @param numero número de línea, empezando en 1 (como se muestra en el .log)
     * @return el texto de esa línea
     */
    public String getLinea(int numero) {
        return lineas.get(numero - 1);
    }

    /**
     * @param numero número de línea, empezando en 1
     * @return el salto de línea original de esa línea: "\r\n", "\n", "\r", o
     *         "" si es la última línea y el archivo no termina en salto de línea
     */
    public String getSaltoDeLinea(int numero) {
        return saltos.get(numero - 1);
    }

    /** @return true si después del último carácter hay un salto de línea. */
    public boolean terminaConSaltoDeLinea() {
        return !saltos.get(saltos.size() - 1).isEmpty();
    }

    /** @return true si el archivo inicia con la marca BOM de UTF-8 (EF BB BF). */
    public boolean tieneBom() {
        return tieneBom;
    }

    /** @return la codificación con la que se leyó el archivo. */
    public Charset getCodificacion() {
        return codificacion;
    }

    /**
     * Salto de línea que se usa para separar renglones que el original no
     * tiene: los errores que van debajo de la última línea cuando esta no
     * termina en salto de línea. Es el salto de la línea más cercana hacia
     * arriba; si el archivo no tiene ninguno, el de Windows.
     *
     * @param numero número de línea, empezando en 1
     * @return el salto de esa línea, o el de la línea anterior más cercana que tenga uno
     */
    public String getSaltoParaSeparar(int numero) {
        for (int n = numero; n >= 1; n--) {
            if (!getSaltoDeLinea(n).isEmpty()) {
                return getSaltoDeLinea(n);
            }
        }
        return SEPARADOR_WINDOWS;
    }
}
