package proyectocompiladores2.validacion;

import java.util.List;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.io.ArchivoFuente;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;

/**
 * R4 - Etiquetas de apertura (<?php) y cierre (?>).
 *
 * Aspecto 1 (apertura): debe escribirse exactamente "<?php" (302) y ser lo
 *   primero del archivo, sin texto, espacios, líneas en blanco ni BOM antes (301).
 *   Si no existe: 303, que se escribe debajo de la línea 0001.
 * Aspecto 2 (cierre obligatorio): debe existir (305, que se escribe debajo de
 *   la última línea) y escribirse exactamente "?>" (304).
 * Aspecto 3 (posición del cierre): debe estar en la última línea, sin nada
 *   después: ni código en la misma línea (306), ni más líneas con código
 *   (307), ni espacios o saltos de línea (308).
 *
 * El AnalizadorLexico reconoce también las formas mal escritas ("<? php",
 * "? >") como etiquetas, para que aquí se pueda comparar con la forma exacta.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorEtiquetas {

    public void validar(List<Token> tokens, ArchivoFuente fuente, GestorErrores errores) {
        validarApertura(tokens, fuente, errores);
        validarCierre(tokens, fuente, errores);
    }

    private void validarApertura(List<Token> tokens, ArchivoFuente fuente, GestorErrores errores) {
        Token apertura = primero(tokens, TipoToken.ETIQUETA_APERTURA);
        if (apertura == null) {
            // Falta la apertura: el error se asocia a la línea 0001, donde debía estar
            errores.registrar(CodigoError.APERTURA_FALTANTE, 1, 0);
            return;
        }
        if (!apertura.getLexema().equals(Patrones.ETIQUETA_APERTURA)) {
            errores.registrar(CodigoError.APERTURA_MAL_ESCRITA, apertura.getLinea(), apertura.getColumna(),
                    apertura.getLexema());
        }

        // Todo lo que hay en el archivo antes de la etiqueta
        StringBuilder previo = new StringBuilder();
        for (int n = 1; n < apertura.getLinea(); n++) {
            previo.append(fuente.getLinea(n)).append('\n');
        }
        previo.append(fuente.getLinea(apertura.getLinea()), 0, apertura.getColumna() - 1);

        if (fuente.tieneBom()) {
            errores.registrar(CodigoError.APERTURA_CONTENIDO_PREVIO, 1, 0, "la marca invisible BOM de 'UTF-8 con BOM' (bytes EF BB BF; guarde el archivo como 'UTF-8' sin BOM)");
        } else if (previo.length() > 0) {
            errores.registrar(CodigoError.APERTURA_CONTENIDO_PREVIO, 1, 0, describirContenido(previo.toString()));
        }
    }

    private void validarCierre(List<Token> tokens, ArchivoFuente fuente, GestorErrores errores) {
        Token cierre = primero(tokens, TipoToken.ETIQUETA_CIERRE);
        if (cierre == null) {
            // Falta el cierre: el error se asocia a la última línea, donde debía estar
            int ultima = fuente.getCantidadLineas();
            errores.registrar(CodigoError.CIERRE_FALTANTE, ultima, fuente.getLinea(ultima).length() + 1,
                    String.format("%04d", ultima));
            return;
        }
        int linea = cierre.getLinea();
        if (!cierre.getLexema().equals(Patrones.ETIQUETA_CIERRE)) {
            errores.registrar(CodigoError.CIERRE_MAL_ESCRITO, linea, cierre.getColumna(), cierre.getLexema());
        }

        // Lo que sigue a la etiqueta en su misma línea y en las líneas siguientes
        String restoDeLinea = fuente.getLinea(linea)
                .substring(cierre.getColumna() - 1 + cierre.getLexema().length());
        String siguienteConTexto = null;
        for (int n = linea + 1; n <= fuente.getCantidadLineas() && siguienteConTexto == null; n++) {
            if (!fuente.getLinea(n).trim().isEmpty()) {
                siguienteConTexto = fuente.getLinea(n).trim();
            }
        }
        boolean hayLineasDespues = linea < fuente.getCantidadLineas();

        if (!restoDeLinea.trim().isEmpty()) {
            errores.registrar(CodigoError.CIERRE_CONTENIDO_POSTERIOR, linea, cierre.getColumna() + 1,
                    restoDeLinea.trim());
        } else if (siguienteConTexto != null) {
            errores.registrar(CodigoError.CIERRE_NO_ES_ULTIMA_LINEA, linea, cierre.getColumna() + 1,
                    siguienteConTexto);
        } else if (!restoDeLinea.isEmpty()) {
            errores.registrar(CodigoError.CIERRE_ESPACIOS_POSTERIORES, linea, cierre.getColumna() + 1,
                    "espacios en blanco");
        } else if (hayLineasDespues || fuente.terminaConSaltoDeLinea()) {
            errores.registrar(CodigoError.CIERRE_ESPACIOS_POSTERIORES, linea, cierre.getColumna() + 1,
                    "saltos de línea");
        }
    }

    /** Describe lo que hay antes de la apertura: texto, espacios o líneas en blanco. */
    private String describirContenido(String contenido) {
        if (!contenido.trim().isEmpty()) {
            for (String linea : contenido.split("\n")) {
                if (!linea.trim().isEmpty()) {
                    return "el texto '" + linea.trim() + "'";
                }
            }
        }
        return contenido.contains("\n") ? "líneas en blanco" : "espacios en blanco";
    }

    private Token primero(List<Token> tokens, TipoToken tipo) {
        for (Token token : tokens) {
            if (token.es(tipo)) {
                return token;
            }
        }
        return null;
    }
}
