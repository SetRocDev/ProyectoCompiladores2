package proyectocompiladores2.validacion;

import java.util.List;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.FlujoTokens;

/**
 * R5 - Comentarios de línea (// y #).
 *
 * Aspecto 1 (reconocimiento): el AnalizadorLexico reconoce "//" y "#" y
 *   deja todo el resto de la línea en un único token COMENTARIO.
 * Aspecto 2 (delimitación): el comentario termina al final de la línea.
 *   Si en la línea siguiente aparece texto suelto (por ejemplo
 *   "continúa aquí"), es un comentario que se quiso extender: Error 402.
 *   Un comentario escrito con una sola barra ("/ texto"): Error 401. Una
 *   '/' al inicio de una línea que continúa una operación de la línea
 *   anterior es una división, no un comentario (lo decide el
 *   AnalizadorLexico; corrección del Proyecto 1, tarea T19).
 * Aspecto 3 (independencia): como el comentario es un solo token, su
 *   contenido ("// if while echo") nunca genera otros tokens ni errores.
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class ValidadorComentarios {

    /**
     * @param tokens  todos los tokens, incluidos los comentarios (para el Error 401)
     * @param flujo   los mismos tokens sin comentarios (para el Error 402)
     * @param errores donde se registran los errores encontrados
     */
    public void validar(List<Token> tokens, FlujoTokens flujo, GestorErrores errores) {
        validarFormaDeComentarios(tokens, errores);
        validarTextoSuelto(flujo, errores);
    }

    /** Error 401: el comentario debe iniciar exactamente con "//" o "#". */
    private void validarFormaDeComentarios(List<Token> tokens, GestorErrores errores) {
        for (Token token : tokens) {
            if (token.es(TipoToken.COMENTARIO) && !Patrones.COMENTARIO.matcher(token.getLexema()).matches()) {
                errores.registrar(CodigoError.COMENTARIO_MAL_FORMADO, token.getLinea(), token.getColumna(),
                        token.getLexema().trim());
            }
        }
    }

    /**
     * Error 402: texto suelto que no es código PHP. Las palabras seguidas de
     * la misma línea se reportan juntas en un solo error.
     */
    private void validarTextoSuelto(FlujoTokens flujo, GestorErrores errores) {
        int i = 0;
        while (i < flujo.tamano()) {
            if (!iniciaTextoSuelto(flujo, i)) {
                i++;
                continue;
            }
            Token primero = flujo.get(i);
            StringBuilder texto = new StringBuilder(primero.getLexema());
            i++;
            while (i < flujo.tamano() && esPalabra(flujo.get(i))
                    && flujo.get(i).getLinea() == primero.getLinea()) {
                texto.append(' ').append(flujo.get(i).getLexema());
                i++;
            }
            errores.registrar(CodigoError.TEXTO_FUERA_DE_COMENTARIO, primero.getLinea(), primero.getColumna(),
                    texto.toString());
        }
    }

    /**
     * Una PALABRA es texto suelto si está al INICIO de una sentencia (antes
     * hay ';', '{', '}', '<?php' o nada) y no es el comienzo de código válido:
     * una llamada a función (le sigue '('), un nombre de clase (le sigue '::')
     * o una palabra clave de PHP. Si le sigue un operador de asignación es un
     * identificador sin '$', que reporta R3 (Error 206).
     *
     * Una palabra en medio de una instrucción (por ejemplo la constante en
     * "echo PHP_EOL;") no es texto suelto. El token anterior y el siguiente
     * se buscan sin importar la línea ni los comentarios (corrección del
     * Proyecto 1, tareas T17 y T19).
     */
    private boolean iniciaTextoSuelto(FlujoTokens flujo, int i) {
        Token token = flujo.get(i);
        if (!token.es(TipoToken.PALABRA) || Patrones.esOtraPalabraClave(token.getLexema())
                || !(flujo.esInicioDeSentencia(i) || sigueAUnValorDeOtraLinea(flujo, i))) {
            return false;
        }
        Token siguiente = flujo.siguiente(i);
        return siguiente == null || !(siguiente.getLexema().equals("(") || siguiente.getLexema().equals("::")
                || Patrones.esOperadorDeAsignacion(siguiente.getLexema()));
    }

    /**
     * Caso "$var = 10 // comentario" y en la línea siguiente "continúa aquí":
     * a la instrucción le falta el ';', pero una palabra que empieza una línea
     * justo después de un valor no puede continuar esa instrucción (en PHP no
     * van dos valores seguidos), así que también es texto suelto.
     */
    private boolean sigueAUnValorDeOtraLinea(FlujoTokens flujo, int i) {
        Token previo = flujo.anterior(i);
        if (previo == null || previo.getLinea() == flujo.get(i).getLinea()) {
            return false;
        }
        String lexema = previo.getLexema();
        return previo.es(TipoToken.IDENTIFICADOR) || previo.es(TipoToken.NUMERO) || previo.es(TipoToken.CADENA)
                || previo.es(TipoToken.PALABRA) || lexema.equals(")") || lexema.equals("]");
    }

    private boolean esPalabra(Token token) {
        return token.es(TipoToken.PALABRA) || token.es(TipoToken.PALABRA_RESERVADA);
    }
}
