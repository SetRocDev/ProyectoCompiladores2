package proyectocompiladores2.sintactico;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import proyectocompiladores2.errores.CodigoError;
import proyectocompiladores2.errores.GestorErrores;
import proyectocompiladores2.io.ArchivoFuente;
import proyectocompiladores2.lexico.Patrones;
import proyectocompiladores2.lexico.TipoToken;
import proyectocompiladores2.lexico.Token;
import proyectocompiladores2.sintactico.arbol.Nodo;
import proyectocompiladores2.sintactico.arbol.NodoBloque;
import proyectocompiladores2.sintactico.arbol.NodoEstructura;
import proyectocompiladores2.sintactico.arbol.NodoPrograma;
import proyectocompiladores2.sintactico.arbol.NodoSentencia;

/**
 * ===== ETAPA 5: ANÁLISIS SINTÁCTICO =====
 *
 * Revisa que los tokens (ya obtenidos, clasificados y validados por las
 * etapas 1 a 4) estén en un orden que respete la gramática del lenguaje, y
 * construye el árbol sintáctico (traducción orientada por la sintaxis).
 *
 * Técnica: ANÁLISIS DESCENDENTE RECURSIVO. Cada regla de la gramática es un
 * método, y una regla que contiene a otra llama a su método:
 *
 *   programa    → '<?php' sentencias '?>'                 analizar()
 *   sentencias  → { sentencia }
 *   sentencia   → while | if | echo | estructura | simple  sentencia()
 *   bloque      → '{' sentencia { sentencia } '}'         bloque()
 *   estructura  → ('for' | 'foreach' | 'function' ...) cabecera bloque
 *                                                         estructuraGenerica()
 *   simple      → token { token } ';'                     sentenciaSimple()
 *
 * Las reglas de while, if y echo están en sus propias clases
 * (ValidadorWhile, ValidadorIf, ValidadorEcho), que usan los métodos
 * públicos de esta clase para avanzar por los tokens y analizar sus bloques.
 *
 * RECUPERACIÓN DE ERRORES (modo pánico): cuando algo no cumple la gramática
 * se registra el error y se sigue analizando desde el siguiente punto
 * seguro (un ';', una '{' o una '}'), para encontrar todos los errores del
 * archivo sin generar errores falsos "en cascada".
 *
 * Uso de IA: estructura generada con apoyo de Claude Code y revisada por el estudiante.
 */
public class AnalizadorSintactico {

    /** Estructuras que el enunciado no pide validar; solo se respetan sus llaves (decisión Q12). */
    private static final Set<String> ESTRUCTURAS_GENERICAS = Patrones.conjunto(
            "for", "foreach", "function", "switch", "class", "interface", "trait",
            "do", "try", "catch", "finally");

    /** Palabras con las que se compara una estructura mal escrita (Error 504). */
    private static final String[] ESTRUCTURAS_CONOCIDAS = {"while", "if", "elseif", "for", "foreach", "switch"};

    private final FlujoTokens flujo;
    private final GestorErrores errores;
    private final ValidadorIndentacion indentacion;

    /** Bloques abiertos en este momento, del más interno al más externo. */
    private final Deque<BloqueAbierto> abiertos = new ArrayDeque<>();

    /** Posición del token actual dentro del flujo. */
    private int posicion;

    /** Un bloque que se está analizando: su tipo y la indentación de la línea que lo abrió. */
    private static final class BloqueAbierto {
        private final TipoBloque tipo;
        private final int anchoCabecera;

        private BloqueAbierto(TipoBloque tipo, int anchoCabecera) {
            this.tipo = tipo;
            this.anchoCabecera = anchoCabecera;
        }
    }

    /**
     * @param flujo   tokens del archivo sin comentarios (etapas 1 a 4)
     * @param fuente  archivo original (para medir la indentación de cada línea)
     * @param errores donde se registran los errores encontrados
     */
    public AnalizadorSintactico(FlujoTokens flujo, ArchivoFuente fuente, GestorErrores errores) {
        this.flujo = flujo;
        this.errores = errores;
        this.indentacion = new ValidadorIndentacion(fuente, errores);
    }

    // =====================================================================
    //  Reglas de la gramática
    // =====================================================================

    /**
     * programa → '<?php' sentencias '?>'
     * Las etiquetas ya las validó la etapa 4 (R4); aquí solo se saltan.
     *
     * @return la raíz del árbol sintáctico
     */
    public NodoPrograma analizar() {
        posicion = 0;
        NodoPrograma programa = new NodoPrograma(1);
        while (!fin()) {
            Token token = actual();
            if (esEtiqueta(token)) {
                avanzar();
            } else if (es(token, "}")) {
                // Una '}' fuera de todo bloque no cierra nada
                registrar(CodigoError.LLAVE_SIN_APERTURA, token);
                avanzar();
            } else {
                programa.agregarHijo(sentencia());
            }
        }
        return programa;
    }

    /**
     * sentencia → while | if | echo | estructura | simple
     * Decide qué regla aplicar según el primer token.
     *
     * @return el nodo de la sentencia reconocida
     */
    public Nodo sentencia() {
        Token token = actual();
        String palabra = token.getLexema().toLowerCase(Locale.ROOT);
        if (es(token, "{")) {
            return bloqueSuelto();
        }
        if (palabra.equals("while") && token.es(TipoToken.PALABRA_RESERVADA)) {
            // Una instancia por while: los while anidados se analizan en forma recursiva
            return new ValidadorWhile().analizar(this);
        }
        if (palabra.equals("echo") && token.es(TipoToken.PALABRA_RESERVADA)) {
            return new ValidadorEcho().analizar(this);
        }
        if (palabra.equals("if") && token.es(TipoToken.PALABRA_RESERVADA)) {
            return new ValidadorIf().analizar(this);
        }
        if (esPalabra(token, "else") || esPalabra(token, "elseif")) {
            // Un else que no viene justo después del bloque de un if
            return new ValidadorIf().analizarElseSinIf(this);
        }
        if (token.es(TipoToken.PALABRA_RESERVADA) || token.es(TipoToken.PALABRA)) {
            if (ESTRUCTURAS_GENERICAS.contains(palabra)) {
                return estructuraGenerica();
            }
            if (token.es(TipoToken.PALABRA) && pareceEstructuraMalEscrita()) {
                registrar(CodigoError.ESTRUCTURA_DESCONOCIDA, token, token.getLexema(), sugerencia(palabra));
                return estructuraGenerica();
            }
            if (token.es(TipoToken.PALABRA) && pareceEchoMalEscrito(token)) {
                // "eco $x;": se reporta como echo mal escrito y se revisa el resto como echo
                reemplazarTextoSuelto(token, "echo");
                return new ValidadorEcho().analizar(this);
            }
            if (token.es(TipoToken.PALABRA) && pareceElseMalEscrito(token)) {
                // "} els {": se reporta como else mal escrito y su bloque se revisa igual
                reemplazarTextoSuelto(token, "else");
                return estructuraGenerica();
            }
        }
        return sentenciaSimple();
    }

    /**
     * simple → token { token } ';'
     *
     * Toma los tokens hasta el ';' (sin contar los que están dentro de
     * paréntesis o corchetes). Si falta el ';', la sentencia termina antes de
     * una llave, del cierre '?>' o de una palabra de estructura que empieza
     * una línea nueva, para no "tragarse" la siguiente instrucción.
     */
    public NodoSentencia sentenciaSimple() {
        NodoSentencia sentencia = new NodoSentencia(actual().getLinea());
        int profundidad = 0;
        boolean primero = true;
        while (!fin()) {
            Token token = actual();
            // Un paréntesis sin cerrar no debe "tragarse" el resto del archivo:
            // ';', '{', '}' y '?>' siempre terminan la sentencia
            if (!primero && (es(token, "{") || es(token, "}") || esEtiqueta(token)
                    || (profundidad == 0 && terminaSentenciaSinPuntoYComa(token)))) {
                break;
            }
            if (es(token, ";")) {
                avanzar();
                break;
            }
            if (es(token, "(") || es(token, "[")) {
                profundidad++;
            } else if ((es(token, ")") || es(token, "]")) && profundidad > 0) {
                profundidad--;
            }
            sentencia.agregarToken(token);
            avanzar();
            primero = false;
        }
        return sentencia;
    }

    /**
     * estructura → palabraClave cabecera ( bloque | ';' | sentencia )
     *
     * Estructuras que no pide el enunciado (for, foreach, function...): se
     * toma la cabecera hasta la '{' y se analiza el bloque, para respetar sus
     * llaves y la indentación de lo que tiene dentro (decisión Q12).
     */
    public NodoEstructura estructuraGenerica() {
        Token palabraClave = actual();
        NodoEstructura estructura = new NodoEstructura(palabraClave.getLinea(),
                palabraClave.getLexema().toLowerCase(Locale.ROOT));
        avanzar();
        int profundidad = 0;
        while (!fin()) {
            Token token = actual();
            if (es(token, "{") || es(token, "}") || esEtiqueta(token)) {
                break;
            }
            // En la cabecera de un for los ';' van dentro de los paréntesis
            if (profundidad == 0 && es(token, ";")) {
                avanzar();
                return estructura;
            }
            if (es(token, "(") || es(token, "[")) {
                profundidad++;
            } else if ((es(token, ")") || es(token, "]")) && profundidad > 0) {
                profundidad--;
            }
            estructura.agregarToken(token);
            avanzar();
        }
        if (!fin() && es(actual(), "{")) {
            estructura.agregarHijo(bloque(TipoBloque.GENERICO, estructura.getPalabraClave(), palabraClave));
            // do { ... } while (condición);  el while final es parte del do
            if (estructura.getPalabraClave().equals("do") && !fin() && esPalabra(actual(), "while")) {
                estructura.agregarHijo(sentenciaSimple());
            }
        }
        return estructura;
    }

    /** Un bloque { } que no pertenece a ninguna estructura. */
    private NodoBloque bloqueSuelto() {
        return bloque(TipoBloque.GENERICO, "{", actual());
    }

    /**
     * bloque → '{' sentencia { sentencia } '}'
     *
     * El token actual debe ser la '{'. Revisa además las reglas comunes de
     * los bloques: que cierre con '}', que no esté vacío y la indentación.
     *
     * @param tipo         tipo de bloque (define los códigos de error)
     * @param palabraClave estructura dueña del bloque, para los mensajes
     * @param cabecera     primer token de la línea que abre el bloque
     *                     (por ejemplo 'while' o la '}' de "} else {")
     * @return el bloque con sus sentencias
     */
    public NodoBloque bloque(TipoBloque tipo, String palabraClave, Token cabecera) {
        return bloque(tipo, palabraClave, cabecera, indentacion.anchoDe(cabecera.getLinea()));
    }

    /**
     * Igual que bloque(tipo, palabraClave, cabecera), pero con la indentación
     * de referencia indicada. La usa el else: su bloque se mide desde la línea
     * del if, aunque la línea "} else {" esté mal indentada.
     *
     * @param anchoReferencia espacios de la línea que abre la estructura
     */
    public NodoBloque bloque(TipoBloque tipo, String palabraClave, Token cabecera, int anchoReferencia) {
        NodoBloque bloque = new NodoBloque(actual().getLinea());
        avanzar(); // la '{'
        contenidoDeBloque(bloque, tipo, palabraClave, cabecera, anchoReferencia);
        return bloque;
    }

    /**
     * Cuerpo de una estructura a la que le falta la '{'. Para no generar
     * errores en cascada se decide, mirando la indentación, cuál de los dos
     * casos es:
     *   - se borró solo la '{' y la '}' sigue más abajo, alineada con la
     *     estructura: se analiza como un bloque normal hasta esa '}';
     *   - la estructura tiene una sola sentencia sin llaves: el cuerpo es
     *     esa sentencia, si está más indentada que la estructura;
     *   - si no, la estructura quedó sin cuerpo y lo que sigue ya está afuera.
     *
     * @return el cuerpo como bloque
     */
    public NodoBloque cuerpoSinLlave(TipoBloque tipo, String palabraClave, Token cabecera, int anchoReferencia) {
        NodoBloque bloque = new NodoBloque(fin() ? cabecera.getLinea() : actual().getLinea());
        if (fin() || esEtiqueta(actual())) {
            return bloque;
        }
        if (tieneLlaveDeCierreAlineada(cabecera, anchoReferencia)) {
            contenidoDeBloque(bloque, tipo, palabraClave, cabecera, anchoReferencia);
        } else if (indentacion.esPrimeroDeSuLinea(actual())
                && indentacion.anchoDe(actual().getLinea()) <= anchoReferencia) {
            // La línea siguiente no está más adentro que la estructura: no es
            // su cuerpo, es código que sigue después (el cuerpo queda vacío)
            return bloque;
        } else {
            abiertos.push(new BloqueAbierto(tipo, anchoReferencia));
            validarIndentacionDeSentencia(actual());
            bloque.agregarHijo(sentencia());
            abiertos.pop();
        }
        return bloque;
    }

    /** Analiza las sentencias de un bloque hasta su '}' y aplica las reglas comunes. */
    private void contenidoDeBloque(NodoBloque bloque, TipoBloque tipo, String palabraClave, Token cabecera,
                                   int anchoReferencia) {
        BloqueAbierto abierto = new BloqueAbierto(tipo, anchoReferencia);
        abiertos.push(abierto);
        boolean cerrado = false;
        while (!fin() && !esEtiquetaDeCierre(actual())) {
            Token token = actual();
            if (es(token, "}")) {
                if (cierraUnBloqueExterior(token, abierto)) {
                    break;
                }
                validarCierre(token, abierto);
                avanzar();
                cerrado = true;
                break;
            }
            if (esEtiqueta(token)) {
                avanzar();
                continue;
            }
            if (terminoElBloqueSinLlave(token, abierto)) {
                break;
            }
            validarIndentacionDeSentencia(token);
            bloque.agregarHijo(sentencia());
        }
        abiertos.pop();
        if (!cerrado) {
            errores.registrar(tipo.getFaltaCierre(), cabecera.getLinea(), cabecera.getColumna(),
                    palabraClave, String.format("%04d", cabecera.getLinea()));
        }
        if (bloque.getHijos().isEmpty() && tipo.getBloqueVacio() != null) {
            errores.registrar(tipo.getBloqueVacio(), cabecera.getLinea(), cabecera.getColumna());
        }
    }

    // =====================================================================
    //  Reglas de indentación y recuperación (comunes a todos los bloques)
    // =====================================================================

    /**
     * La primera línea de cada sentencia dentro de un bloque lleva 4 espacios
     * más que la línea que abrió el bloque. Los códigos de error son los de
     * la estructura validada más cercana (while, if o else); el bloque de un
     * for, foreach o function que no está dentro de ninguna de ellas no se
     * revisa (decisión Q9, confirmada por la profesora).
     */
    private void validarIndentacionDeSentencia(Token primero) {
        BloqueAbierto actual = abiertos.peek();
        TipoBloque reglas = reglasDeIndentacion();
        if (actual != null && reglas != null) {
            indentacion.validarSentencia(primero,
                    actual.anchoCabecera + ValidadorIndentacion.ESPACIOS_POR_NIVEL, reglas);
        }
    }

    /** La '}' va alineada con la línea que abrió el bloque. */
    private void validarCierre(Token llave, BloqueAbierto bloque) {
        TipoBloque reglas = reglasDeIndentacion();
        if (reglas != null) {
            indentacion.validarCierre(llave, bloque.anchoCabecera, reglas);
        }
    }

    /** @return las reglas del bloque validado (while, if, else) más interno, o null si no hay. */
    private TipoBloque reglasDeIndentacion() {
        for (BloqueAbierto bloque : abiertos) {
            if (bloque.tipo.validaIndentacion()) {
                return bloque.tipo;
            }
        }
        return null;
    }

    /**
     * Una '}' que empieza su línea con MENOS espacios que el bloque actual, y
     * alineada con un bloque de afuera, cierra ese bloque de afuera: al bloque
     * actual le falta su propia '}'. Así, si falta la '}' de un if que está
     * dentro de un while, el error se reporta en el if y no en el while.
     */
    private boolean cierraUnBloqueExterior(Token llave, BloqueAbierto actual) {
        if (!indentacion.esPrimeroDeSuLinea(llave)) {
            return false;
        }
        int ancho = indentacion.anchoDe(llave.getLinea());
        return ancho < actual.anchoCabecera && esAnchoDeBloqueExterior(ancho);
    }

    /**
     * Una sentencia que vuelve al nivel de la cabecera (o menos) puede ser:
     *   - una línea mal indentada que sigue dentro del bloque (el bloque
     *     todavía tiene su '}' más abajo): se reporta la indentación;
     *   - código que ya está fuera del bloque porque a este le falta su '}':
     *     no queda ninguna '}' libre que le corresponda, la que queda está
     *     alineada con un bloque de afuera, o es la de un "} else", que solo
     *     puede cerrar un if.
     * En el segundo caso el bloque termina aquí, para no reportar como mal
     * indentado todo el código que sigue (error en cascada).
     */
    private boolean terminoElBloqueSinLlave(Token token, BloqueAbierto actual) {
        if (!indentacion.esPrimeroDeSuLinea(token) || indentacion.anchoDe(token.getLinea()) > actual.anchoCabecera) {
            return false;
        }
        int indice = indiceDeLlaveLibre();
        if (indice < 0) {
            return true;
        }
        if (actual.tipo != TipoBloque.IF && indice + 1 < flujo.tamano()
                && (esPalabra(flujo.get(indice + 1), "else") || esPalabra(flujo.get(indice + 1), "elseif"))) {
            return true;
        }
        int ancho = indentacion.anchoDe(flujo.get(indice).getLinea());
        if (ancho == actual.anchoCabecera && hayEstructuraSinLlaveAntes(indice, ancho)) {
            return true;
        }
        return ancho != actual.anchoCabecera && esAnchoDeBloqueExterior(ancho);
    }

    /**
     * Entre la posición actual y la '}' libre, ¿hay una estructura alineada
     * con esa '}' que no abrió su '{'? Por ejemplo "while (...)" sin '{' y
     * su '}' más abajo: esa '}' es del while, no del bloque actual (al que le
     * falta su propia '}').
     */
    private boolean hayEstructuraSinLlaveAntes(int indiceLlave, int ancho) {
        for (int i = posicion; i < indiceLlave; i++) {
            Token token = flujo.get(i);
            boolean esEstructura = esPalabra(token, "while") || esPalabra(token, "for")
                    || esPalabra(token, "if") || esPalabra(token, "elseif") || esPalabra(token, "foreach")
                    || esPalabra(token, "else");
            if (esEstructura && indentacion.esPrimeroDeSuLinea(token)
                    && indentacion.anchoDe(token.getLinea()) == ancho && !abreSuLlave(i)) {
                return true;
            }
        }
        return false;
    }

    /** @return true si a la palabra de la posición indicada le sigue su '{' (después de la cabecera). */
    private boolean abreSuLlave(int indicePalabra) {
        int i = indicePalabra + 1;
        if (i < flujo.tamano() && es(flujo.get(i), "(")) {
            int profundidad = 0;
            for (; i < flujo.tamano(); i++) {
                Token token = flujo.get(i);
                if (es(token, "(")) {
                    profundidad++;
                } else if (es(token, ")") && --profundidad == 0) {
                    i++;
                    break;
                } else if (es(token, "{") || es(token, "}") || es(token, ";")) {
                    break;
                }
            }
        }
        return i < flujo.tamano() && (es(flujo.get(i), "{") || esPalabra(flujo.get(i), "if"));
    }

    /**
     * @return la posición de la siguiente '}' que no cierra ningún bloque
     *         abierto más adelante (contando las llaves desde la posición
     *         actual), o -1 si no hay
     */
    private int indiceDeLlaveLibre() {
        int profundidad = 0;
        for (int i = posicion; i < flujo.tamano(); i++) {
            Token token = flujo.get(i);
            if (esEtiquetaDeCierre(token)) {
                return -1;
            }
            if (es(token, "{")) {
                profundidad++;
            } else if (es(token, "}")) {
                if (profundidad == 0) {
                    return i;
                }
                profundidad--;
            }
        }
        return -1;
    }

    /** @return true si algún bloque abierto, aparte del actual, tiene esa indentación. */
    private boolean esAnchoDeBloqueExterior(int ancho) {
        Iterator<BloqueAbierto> externos = abiertos.iterator();
        externos.next(); // el actual
        while (externos.hasNext()) {
            if (externos.next().anchoCabecera == ancho) {
                return true;
            }
        }
        return false;
    }

    /**
     * Para el caso de una estructura sin '{': busca la primera línea, después
     * de la cabecera, que tenga la misma indentación que la cabecera o menos.
     *
     * @return true si esa línea empieza con una '}' alineada con la cabecera
     */
    private boolean tieneLlaveDeCierreAlineada(Token cabecera, int anchoCabecera) {
        for (int i = posicion; i < flujo.tamano(); i++) {
            Token token = flujo.get(i);
            if (token.getLinea() > cabecera.getLinea() && indentacion.esPrimeroDeSuLinea(token)
                    && indentacion.anchoDe(token.getLinea()) <= anchoCabecera) {
                return es(token, "}") && indentacion.anchoDe(token.getLinea()) == anchoCabecera;
            }
        }
        return false;
    }

    /** Si falta el ';', la sentencia termina antes de estos tokens. */
    private boolean terminaSentenciaSinPuntoYComa(Token token) {
        if (es(token, "{") || es(token, "}") || esEtiqueta(token)) {
            return true;
        }
        String palabra = token.getLexema().toLowerCase(Locale.ROOT);
        boolean esEstructura = ESTRUCTURAS_GENERICAS.contains(palabra) || palabra.equals("while")
                || palabra.equals("if") || palabra.equals("else") || palabra.equals("elseif")
                || palabra.equals("echo");
        return esEstructura && (token.es(TipoToken.PALABRA_RESERVADA) || token.es(TipoToken.PALABRA))
                && indentacion.esPrimeroDeSuLinea(token);
    }

    /**
     * Una palabra que no es palabra clave, seguida de '(' ... ')' y luego de
     * '{', tiene la forma de una estructura de control ("whlie ($x) {"). Una
     * llamada a función nunca va seguida de un bloque.
     */
    private boolean pareceEstructuraMalEscrita() {
        if (posicion + 1 >= flujo.tamano() || !es(flujo.get(posicion + 1), "(")) {
            return false;
        }
        int profundidad = 0;
        for (int i = posicion + 1; i < flujo.tamano(); i++) {
            Token token = flujo.get(i);
            if (es(token, "(")) {
                profundidad++;
            } else if (es(token, ")")) {
                profundidad--;
                if (profundidad == 0) {
                    return i + 1 < flujo.tamano() && es(flujo.get(i + 1), "{");
                }
            } else if (es(token, ";") || es(token, "{") || es(token, "}")) {
                return false;
            }
        }
        return false;
    }

    /**
     * "eco $x;" o "ehco "Hola";": una palabra que se parece a echo (una o dos
     * letras de diferencia) seguida directamente de un valor. En PHP una
     * palabra suelta nunca va seguida de un valor, así que es un echo mal
     * escrito.
     */
    private boolean pareceEchoMalEscrito(Token palabra) {
        Token siguiente = siguiente();
        return seParece(palabra.getLexema(), "echo") && siguiente != null
                && (siguiente.es(TipoToken.CADENA) || siguiente.es(TipoToken.IDENTIFICADOR)
                        || siguiente.es(TipoToken.NUMERO));
    }

    /** "} els {": una palabra que se parece a else seguida directamente de '{'. */
    private boolean pareceElseMalEscrito(Token palabra) {
        return seParece(palabra.getLexema(), "else") && es(siguiente(), "{");
    }

    /** Al menos 3 letras y a lo sumo 2 de diferencia con la palabra clave. */
    private static boolean seParece(String palabra, String palabraClave) {
        String minuscula = palabra.toLowerCase(Locale.ROOT);
        return minuscula.length() >= 3 && !minuscula.equals(palabraClave)
                && Patrones.distanciaDeEdicion(minuscula, palabraClave) <= 2;
    }

    /**
     * La etapa 4 ya reportó la palabra como texto suelto (402); como aquí se
     * sabe que es una palabra clave mal escrita, se reemplaza por el 504, que
     * es más específico (un error, un mensaje).
     */
    private void reemplazarTextoSuelto(Token palabra, String palabraClave) {
        errores.quitar(CodigoError.TEXTO_FUERA_DE_COMENTARIO, palabra.getLinea(), palabra.getColumna());
        registrar(CodigoError.ESTRUCTURA_DESCONOCIDA, palabra, palabra.getLexema(),
                "; ¿quiso decir '" + palabraClave + "'?");
    }

    /** @return "; ¿quiso decir 'while'?" si la palabra se parece a una estructura conocida. */
    private String sugerencia(String palabra) {
        for (String conocida : ESTRUCTURAS_CONOCIDAS) {
            if (Patrones.distanciaDeEdicion(palabra, conocida) <= 2) {
                return "; ¿quiso decir '" + conocida + "'?";
            }
        }
        return "";
    }


    // =====================================================================
    //  Movimiento por los tokens (lo usan también ValidadorWhile, If y Echo)
    // =====================================================================

    /** @return true si ya no quedan tokens. */
    public boolean fin() {
        return posicion >= flujo.tamano();
    }

    /** @return el token actual (no avanza). */
    public Token actual() {
        return flujo.get(posicion);
    }

    /** @return el token que sigue al actual, o null si no hay. */
    public Token siguiente() {
        return flujo.siguiente(posicion);
    }

    /** @return el token que está n posiciones después del actual, o null si no hay. */
    public Token adelante(int n) {
        return posicion + n < flujo.tamano() ? flujo.get(posicion + n) : null;
    }

    /** @return el token anterior al actual, o null si no hay. */
    public Token anterior() {
        return flujo.anterior(posicion);
    }

    /** Pasa al siguiente token. */
    public void avanzar() {
        posicion++;
    }

    /** @return true si el token es exactamente ese lexema. */
    public static boolean es(Token token, String lexema) {
        return token != null && token.getLexema().equals(lexema);
    }

    /** @return true si el token es esa palabra (sin distinguir mayúsculas, como PHP). */
    public static boolean esPalabra(Token token, String palabra) {
        return token != null && token.getLexema().equalsIgnoreCase(palabra)
                && (token.es(TipoToken.PALABRA_RESERVADA) || token.es(TipoToken.PALABRA));
    }

    private static boolean esEtiqueta(Token token) {
        return token.es(TipoToken.ETIQUETA_APERTURA) || token.es(TipoToken.ETIQUETA_CIERRE);
    }

    private static boolean esEtiquetaDeCierre(Token token) {
        return token.es(TipoToken.ETIQUETA_CIERRE);
    }

    /** Registra un error en la posición de un token. */
    public void registrar(CodigoError codigo, Token token, Object... datos) {
        errores.registrar(codigo, token.getLinea(), token.getColumna(), datos);
    }

    public GestorErrores getErrores() {
        return errores;
    }

    public ValidadorIndentacion getIndentacion() {
        return indentacion;
    }
}
