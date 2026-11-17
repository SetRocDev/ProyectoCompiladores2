package proyectocompiladores2.sintactico;

import java.util.Arrays;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.io.ArchivoFuente;
import proyectocompiladores2.lexico.Token;

/**
 * Regla de indentación común a los bloques del while, del if y del else
 * (decisión Q9, confirmada por la profesora):
 *   - exactamente 4 espacios por nivel: cada línea de código dentro de un
 *     bloque lleva 4 espacios más que la línea que abre el bloque;
 *   - no se permiten tabulaciones;
 *   - la llave de cierre '}' va alineada con la línea que abrió el bloque;
 *   - si la llave de apertura '{' o el 'else' van solos en su propia línea
 *     (la profesora aclaró que se permite), también van alineados con ella.
 *
 * Solo se revisa la PRIMERA línea de cada sentencia (una sentencia que
 * continúa en la línea siguiente puede llevar más espacios). Las líneas en
 * blanco y los comentarios no se revisan, porque no son tokens del flujo.
 * Se reporta un solo error por línea: si hay una tabulación, solo ese.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorIndentacion {

    /** Espacios por cada nivel de anidamiento, según el enunciado. */
    public static final int ESPACIOS_POR_NIVEL = 4;

    private final ArchivoFuente fuente;
    private final GestorErrores errores;

    public ValidadorIndentacion(ArchivoFuente fuente, GestorErrores errores) {
        this.fuente = fuente;
        this.errores = errores;
    }

    /**
     * @param numeroLinea número de línea (desde 1)
     * @return los espacios y tabulaciones al inicio de esa línea
     */
    public String indentacionDe(int numeroLinea) {
        String linea = fuente.getLinea(numeroLinea);
        int i = 0;
        while (i < linea.length() && (linea.charAt(i) == ' ' || linea.charAt(i) == '\t')) {
            i++;
        }
        return linea.substring(0, i);
    }

    /** @return cantidad de caracteres de indentación de la línea (espacios y tabulaciones). */
    public int anchoDe(int numeroLinea) {
        return indentacionDe(numeroLinea).length();
    }

    /** @return true si no hay nada antes del token en su línea (solo espacios o tabulaciones). */
    public boolean esPrimeroDeSuLinea(Token token) {
        return token.getColumna() - 1 == anchoDe(token.getLinea());
    }

    /**
     * Revisa la línea donde empieza una sentencia dentro de un bloque.
     *
     * @param primero   primer token de la sentencia
     * @param esperados espacios que debe tener la línea
     * @param reglas    tipo de bloque que define los códigos de error
     */
    public void validarSentencia(Token primero, int esperados, TipoBloque reglas) {
        if (!esPrimeroDeSuLinea(primero)) {
            return;
        }
        String indentacion = indentacionDe(primero.getLinea());
        if (indentacion.indexOf('\t') >= 0) {
            errores.registrar(reglas.getTabulacion(), primero.getLinea(), 1);
        } else if (indentacion.length() != esperados) {
            errores.registrar(reglas.getIndentacion(), primero.getLinea(), 1, esperados, indentacion.length());
        }
    }

    /**
     * Revisa la llave '}' que cierra un bloque: debe estar alineada con la
     * línea que lo abrió.
     *
     * @param llave     token '}'
     * @param esperados espacios que tiene la línea que abrió el bloque
     * @param reglas    tipo de bloque que define los códigos de error
     */
    public void validarCierre(Token llave, int esperados, TipoBloque reglas) {
        validarAlineacion(llave, esperados, reglas.getCierreDesalineado(), reglas.getTabulacion());
    }

    /**
     * Revisa que un token que empieza su línea (una '}', una '{' sola en su
     * línea o un 'else' en su propia línea) esté alineado con la estructura.
     * Si no es el primero de su línea no se revisa: por ejemplo, la '}' de
     * "} else {" o la '{' al final de "while (...) {".
     *
     * @param esperados    espacios que tiene la línea de la estructura
     * @param desalineado  error si la cantidad de espacios no coincide; su
     *                     descripción recibe (datosPrevios..., esperados, encontrados)
     * @param tabulacion   error si la indentación tiene tabulaciones
     * @param datosPrevios datos de la descripción antes de los espacios
     */
    public void validarAlineacion(Token token, int esperados, CodigoError desalineado, CodigoError tabulacion,
                                  Object... datosPrevios) {
        if (!esPrimeroDeSuLinea(token)) {
            return;
        }
        String indentacion = indentacionDe(token.getLinea());
        if (indentacion.indexOf('\t') >= 0) {
            errores.registrar(tabulacion, token.getLinea(), 1);
        } else if (indentacion.length() != esperados) {
            Object[] datos = Arrays.copyOf(datosPrevios, datosPrevios.length + 2);
            datos[datosPrevios.length] = esperados;
            datos[datosPrevios.length + 1] = indentacion.length();
            errores.registrar(desalineado, token.getLinea(), 1, datos);
        }
    }
}
