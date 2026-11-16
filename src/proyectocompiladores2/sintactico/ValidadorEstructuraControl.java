package proyectocompiladores2.sintactico;

import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.NodoBloque;
import proyectocompiladores2.sintactico.arbol.NodoCondicion;

/**
 * Parte común de las estructuras de control con condición: while e if.
 * Las dos tienen la misma forma:
 *
 *     palabraClave '(' condicion ')' '{' sentencias '}'
 *
 * Esta clase revisa esa forma una sola vez (POO: herencia). ValidadorWhile y
 * ValidadorIf solo agregan lo propio de cada una (el if agrega el else). Los
 * números de error salen del TipoBloque de cada estructura (6xx el while,
 * 7xx el if).
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public abstract class ValidadorEstructuraControl {

    /** Último token de la cabecera (el ')' o, si falta, el último de la condición). */
    private Token ultimoDeCabecera;

    /**
     * cabecera → palabraClave '(' condicion ')'
     *
     * El token actual debe ser la palabra clave. Al terminar, el token actual
     * es lo que sigue a la cabecera (normalmente la '{').
     *
     * Aspecto "estructura sintáctica" del enunciado:
     *   - debe tener paréntesis de apertura y de cierre;
     *   - la condición debe existir, dentro de los paréntesis y antes de la
     *     llave (para este proyecto se asume correcta: solo se valida que esté).
     *
     * @return la condición (los tokens entre los paréntesis)
     */
    protected NodoCondicion analizarCabecera(AnalizadorSintactico analizador, TipoBloque reglas) {
        Token palabraClave = analizador.actual();
        analizador.avanzar();
        NodoCondicion condicion = new NodoCondicion(palabraClave.getLinea());
        ultimoDeCabecera = palabraClave;

        boolean tieneApertura = !analizador.fin() && AnalizadorSintactico.es(analizador.actual(), "(");
        if (tieneApertura) {
            ultimoDeCabecera = analizador.actual();
            analizador.avanzar();
        } else {
            analizador.registrar(reglas.getFaltaParentesisApertura(), palabraClave);
        }

        // La condición: hasta el ')' que cierra (contando paréntesis internos)
        // o, si falta, hasta la '{', un ';' o una '}'.
        int profundidad = 0;
        boolean tieneCierre = false;
        while (!analizador.fin()) {
            Token token = analizador.actual();
            if (profundidad == 0 && terminaCabecera(token)) {
                break;
            }
            if (AnalizadorSintactico.es(token, "(")) {
                profundidad++;
            } else if (AnalizadorSintactico.es(token, ")")) {
                if (profundidad == 0) {
                    tieneCierre = true;
                    ultimoDeCabecera = token;
                    analizador.avanzar();
                    break;
                }
                profundidad--;
            }
            condicion.agregarToken(token);
            ultimoDeCabecera = token;
            analizador.avanzar();
        }

        if (!tieneCierre) {
            analizador.registrar(reglas.getFaltaParentesisCierre(), ultimoDeCabecera);
        }
        if (condicion.getTokens().isEmpty()) {
            analizador.registrar(reglas.getCondicionVacia(), palabraClave);
        }
        return condicion;
    }

    /**
     * cuerpo → '{' sentencias '}'
     *
     * Revisa que la '{' exista y esté en la misma línea que la condición
     * (decisión Q7), y analiza el bloque con las reglas comunes (cierre,
     * bloque vacío, indentación).
     *
     * @param cabecera primer token de la línea que abre el bloque (la palabra clave)
     * @return el bloque
     */
    protected NodoBloque analizarCuerpo(AnalizadorSintactico analizador, TipoBloque reglas,
                                       String palabraClave, Token cabecera) {
        if (!analizador.fin() && AnalizadorSintactico.es(analizador.actual(), "{")) {
            Token llave = analizador.actual();
            if (llave.getLinea() != ultimoDeCabecera.getLinea()) {
                analizador.registrar(reglas.getAperturaEnOtraLinea(), llave);
            }
            return analizador.bloque(reglas, palabraClave, cabecera);
        }
        analizador.registrar(reglas.getFaltaApertura(), ultimoDeCabecera);
        return analizador.cuerpoSinLlave(reglas, palabraClave, cabecera);
    }

    /** @return el último token de la cabecera analizada (para ubicar errores). */
    protected Token getUltimoDeCabecera() {
        return ultimoDeCabecera;
    }

    /** La condición termina, si falta el ')', antes de la '{', un ';', una '}' o el cierre '?>'. */
    private boolean terminaCabecera(Token token) {
        return AnalizadorSintactico.es(token, "{") || AnalizadorSintactico.es(token, ";")
                || AnalizadorSintactico.es(token, "}") || token.es(TipoToken.ETIQUETA_CIERRE)
                || token.es(TipoToken.ETIQUETA_APERTURA);
    }
}
