package proyectocompiladores2.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import proyectocompiladores2.errores.ErrorCompilacion;
import proyectocompiladores2.errores.GestorErrores;

/**
 * Genera el archivo de errores "nombreArchivo-errores.log" (R2).
 *
 * Formato (decisiones Q2 y Q3):
 *   - Cada línea del .php se copia sin ningún cambio, precedida por su número
 *     de 4 dígitos y un espacio: "0001 <?php".
 *   - Los errores de una línea se escriben justo debajo de ella, uno por
 *     renglón: "Error 201. Descripción" (Opción 1 del enunciado).
 *   - No hay errores "sueltos" al final: todo error tiene su línea, de forma
 *     consistente en todo el archivo (corrección del Proyecto 1, tarea T16).
 *
 * El .log se guarda en la misma carpeta del .php, con la misma codificación
 * (UTF-8, con su BOM si el .php lo tiene, o la de Windows si se guardó como
 * "ANSI") y con el salto de línea original de cada línea (ver
 * generarContenido), para que sea una copia exacta. Si ya existe se reemplaza.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class EscritorLog {

    /** Sufijo que se agrega al nombre del archivo original. */
    public static final String SUFIJO = "-errores.log";

    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * @param fuente  archivo .php leído
     * @return ruta del .log que corresponde a ese archivo
     */
    public Path rutaDelLog(ArchivoFuente fuente) {
        return fuente.getRuta().resolveSibling(fuente.getNombreBase() + SUFIJO);
    }

    /**
     * Escribe el .log con todas las líneas numeradas y sus errores.
     *
     * @param fuente  archivo .php leído
     * @param errores errores encontrados durante el análisis
     * @return ruta del .log generado
     * @throws IOException si no se puede escribir el archivo
     */
    public Path escribir(ArchivoFuente fuente, GestorErrores errores) throws IOException {
        Path destino = rutaDelLog(fuente);
        // Un carácter que no exista en la codificación del .php se escribe como '?'
        CharsetEncoder codificador = fuente.getCodificacion().newEncoder()
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .onMalformedInput(CodingErrorAction.REPLACE);
        try (OutputStream bytes = Files.newOutputStream(destino)) {
            if (fuente.tieneBom()) {
                bytes.write(BOM_UTF8);
            }
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(bytes, codificador))) {
                out.write(generarContenido(fuente, errores));
            }
        }
        return destino;
    }

    /**
     * Arma el contenido completo del .log.
     *
     * Regla de los saltos de línea (corrección del Proyecto 1, tarea T15):
     * cada línea del .php se copia con SU salto de línea original (un archivo
     * puede mezclar CRLF de Windows, LF de Linux y CR), y los errores que van
     * debajo de una línea usan ese mismo salto. Así el .log reproduce el
     * formato exacto del original. Si la última línea del .php no tiene salto
     * de línea, el .log tampoco termina en salto de línea; si esa línea tiene
     * errores, se separa de ellos con el salto de la línea anterior.
     *
     * @return el texto del .log
     */
    public String generarContenido(ArchivoFuente fuente, GestorErrores errores) {
        List<String> renglones = new ArrayList<>();
        List<String> saltos = new ArrayList<>();
        int ultima = fuente.getCantidadLineas();
        for (int numero = 1; numero <= ultima; numero++) {
            String salto = fuente.getSaltoDeLinea(numero);
            renglones.add(numerar(numero, fuente.getLinea(numero)));
            saltos.add(salto);
            for (ErrorCompilacion error : errores.erroresDeLinea(numero)) {
                renglones.add(error.toString());
                saltos.add(salto);
            }
        }

        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < renglones.size(); i++) {
            contenido.append(renglones.get(i));
            boolean esElUltimoRenglon = i == renglones.size() - 1;
            if (!saltos.get(i).isEmpty()) {
                contenido.append(saltos.get(i));
            } else if (!esElUltimoRenglon) {
                // La última línea del .php no tiene salto pero debajo van sus errores
                contenido.append(fuente.getSaltoParaSeparar(ultima));
            }
        }
        return contenido.toString();
    }

    /**
     * @return la línea precedida por su número de 4 dígitos y un espacio,
     *         por ejemplo numerar(1, "<?php") devuelve "0001 <?php"
     */
    public String numerar(int numero, String linea) {
        return String.format("%04d %s", numero, linea);
    }
}
