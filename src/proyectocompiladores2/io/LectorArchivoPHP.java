package proyectocompiladores2.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un archivo .php respetando su formato original en su totalidad (R1).
 *
 * Se lee el archivo completo como bytes, en lugar de usar readLine(), porque
 * readLine() pierde información que el analizador necesita: el salto de
 * línea exacto de cada línea (CRLF, LF o CR, que pueden venir mezclados), si
 * el archivo termina con un salto de línea y si tiene BOM. Esa información
 * permite que el .log sea una copia exacta (R2) y validar que no exista nada
 * después de la etiqueta '?>' (R4).
 *
 * Codificación: se intenta UTF-8 estricto; si el archivo tiene bytes que no
 * son UTF-8 válido (por ejemplo, si se guardó como "ANSI" en el Bloc de
 * notas), se lee como Windows-1252 para no perder las tildes.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class LectorArchivoPHP {

    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * @param ruta archivo a leer (debe tener extensión .php)
     * @return el contenido con su información de formato
     * @throws IOException si el archivo no se puede leer
     */
    public ArchivoFuente leer(Path ruta) throws IOException {
        byte[] bytes = Files.readAllBytes(ruta);

        boolean tieneBom = iniciaConBom(bytes);
        int inicio = tieneBom ? BOM_UTF8.length : 0;

        String texto;
        try {
            texto = decodificarEstricto(bytes, inicio, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            texto = new String(bytes, inicio, bytes.length - inicio, Charset.forName("windows-1252"));
        }

        List<String> lineas = new ArrayList<>();
        List<String> saltos = new ArrayList<>();
        separarLineas(texto, lineas, saltos);
        return new ArchivoFuente(ruta, lineas, saltos, tieneBom);
    }

    /**
     * Divide el texto en líneas y guarda el salto de línea REAL de cada una
     * (CRLF de Windows, LF de Linux o CR de Mac antiguo). Un mismo archivo
     * puede mezclarlos, y el .log debe reproducirlos tal cual (corrección del
     * Proyecto 1, tarea T15).
     *
     * Si el archivo termina en salto de línea no se agrega una línea vacía
     * extra: ese dato queda en el salto de la última línea. Un archivo vacío
     * tiene una sola línea vacía sin salto.
     */
    private void separarLineas(String texto, List<String> lineas, List<String> saltos) {
        int inicioLinea = 0;
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c != '\r' && c != '\n') {
                i++;
                continue;
            }
            lineas.add(texto.substring(inicioLinea, i));
            if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                saltos.add("\r\n");
                i += 2;
            } else {
                saltos.add(String.valueOf(c));
                i++;
            }
            inicioLinea = i;
        }
        if (inicioLinea < texto.length() || lineas.isEmpty()) {
            lineas.add(texto.substring(inicioLinea));
            saltos.add("");
        }
    }

    private boolean iniciaConBom(byte[] bytes) {
        return bytes.length >= 3 && bytes[0] == BOM_UTF8[0]
                && bytes[1] == BOM_UTF8[1] && bytes[2] == BOM_UTF8[2];
    }

    /** Decodifica fallando ante bytes inválidos, en vez de reemplazarlos con '?'. */
    private String decodificarEstricto(byte[] bytes, int inicio, Charset charset)
            throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, inicio, bytes.length - inicio))
                .toString();
    }
}
