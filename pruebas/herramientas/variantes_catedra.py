"""Variantes del codigo PHP de la catedra (recursos/CodigoProyecto.php).

Cada variante cambia el codigo de la catedra como lo haria el tutor y declara
los errores EXACTOS que el analizador debe reportar (ni uno mas, ni uno menos),
segun el enunciado, la retroalimentacion del Proyecto 1 y las aclaraciones de
la profesora.

Usos:
  python3 variantes_catedra.py JAR --carpeta DESTINO
      Genera la carpeta de pruebas: DESTINO/casos/*.php, DESTINO/esperados/*-errores.log
      y DESTINO/esperado.txt. Solo la escribe si TODAS las variantes dan exactamente
      sus errores esperados.
  python3 variantes_catedra.py JAR --individuales TRABAJO
      Prueba cada variante por separado.
  python3 variantes_catedra.py JAR --masivo CANTIDAD ERRORES_POR_CASO SEMILLA TRABAJO
      Combinaciones al azar de variantes compatibles (2 a 5 errores por archivo).

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import collections
import os
import random
import re
import shutil
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
B = open(os.path.join(RAIZ, 'recursos', 'CodigoProyecto.php'), 'rb').read().decode('utf-8').split('\r\n')


def L(n):
    return B[n - 1]


def ind(n):
    t = L(n)
    return t[:len(t) - len(t.lstrip())]


Variante = collections.namedtuple('Variante', 'nombre cambios contenido esperados descripcion etiquetas')
VARIANTES = []
_contador = collections.Counter()


def v(grupo, slug, cambios, esperados, descripcion, etiquetas=(), contenido=None):
    """cambios: {linea: nuevo texto | lista de lineas | None (borrar)}; contenido: bytes del archivo completo."""
    _contador[grupo] += 1
    nombre = f'{grupo}{_contador[grupo]:03d}_{slug}'
    VARIANTES.append(Variante(nombre, cambios, contenido, list(esperados), descripcion, set(etiquetas)))


def aplicar(cambios):
    lineas = list(B)
    for i, nuevo in sorted(cambios.items(), reverse=True):
        if nuevo is None:
            del lineas[i - 1]
        elif isinstance(nuevo, list):
            lineas[i - 1:i] = nuevo
        else:
            lineas[i - 1] = nuevo
    return '\r\n'.join(lineas).encode('utf-8')


# =====================================================================================
# ID  Identificadores (R3 del Proyecto 1): solo la variable que RECIBE el valor
# =====================================================================================
ASIGNACIONES = {5: '$nombre_estudiante', 6: '$cantidad_notas', 9: '$notas', 16: '$suma_notas',
                19: '$suma_notas', 22: '$promedio', 35: '$contador_revision', 39: '$contador_revision',
                43: '$mensaje_final', 47: '$total', 48: '$Total', 50: '$resultado'}
# primera linea en que se USA cada variable (despues de asignarla); las lineas 19 y 39 reasignan
USADA_DESPUES = {5: True, 6: True, 9: True, 16: True, 19: False, 22: True, 35: True, 39: False,
                 43: True, 47: True, 48: True, 50: True}
for n, var in ASIGNACIONES.items():
    nom = var[1:]
    t = L(n)
    k = t.index(var)
    def con(nuevo):
        return t[:k] + nuevo + t[k + len(var):]
    eti = {f'var:{var.lower()}'}
    v('ID', f'L{n:02d}_digito', {n: con('$1' + nom)}, ['201'], f"L{n}: '{var}' -> '$1{nom}' (digito despues de $)", eti)
    for s, slug in (('-', 'guion'), ('@', 'arroba'), ('+', 'mas'), ('!', 'admiracion')):
        v('ID', f'L{n:02d}_simbolo_{slug}', {n: con('$' + s + nom)}, ['202'],
          f"L{n}: '{var}' -> '${s}{nom}' (simbolo despues de $)", eti)
    for s, slug in (('@', 'arroba'), ('ñ', 'enie'), ('$', 'dolar')):
        v('ID', f'L{n:02d}_caracter_{slug}', {n: con('$' + nom[:2] + s + nom[2:])}, ['203'],
          f"L{n}: '{var}' -> '${nom[:2]}{s}{nom[2:]}' (caracter invalido en el nombre)", eti)
    v('ID', f'L{n:02d}_sin_dolar', {n: con(nom)}, ['206'], f"L{n}: '{var}' -> '{nom}' (sin $)", eti)
    v('ID', f'L{n:02d}_dolar_solo', {n: con('$')}, ['205', '901'] if USADA_DESPUES[n] else ['205'],
      f"L{n}: '{var}' -> '$' ($ sin nombre; la variable original queda sin valor: 901)" if USADA_DESPUES[n]
      else f"L{n}: '{var}' -> '$' ($ sin nombre)", eti)
for p in ['class', 'function', 'if', 'while', 'echo', 'else', 'continue', 'for', 'return', 'public', 'null',
          'false', 'true', 'trim', 'foreach', 'private', 'include', 'print', 'const', 'do',
          'IF', 'While', 'ECHO', 'Trim', 'NULL']:
    v('ID', f'reservada_{p}', {46: [L(46), f'${p} = 1;']}, ['204'], f"Despues de L46: '${p} = 1;' (palabra reservada)")
for op in ['+=', '-=', '*=', '/=', '.=', '%=']:
    slug = {'+=': 'mas', '-=': 'menos', '*=': 'por', '/=': 'entre', '.=': 'punto', '%=': 'modulo'}[op]
    v('ID', f'compuesta_{slug}', {50: [L(50), f'$1resultado {op} 2;']}, ['201'],
      f"Despues de L50: '$1resultado {op} 2;' (asignacion compuesta)", {'var:$resultado'})
v('ID', 'reservada_en_dos_lineas', {46: [L(46), '$for', '    = 5;']}, ['204'], "Despues de L46: '$for' y '= 5;' en lineas distintas")
v('ID', 'sin_dolar_en_dos_lineas', {46: [L(46), 'contador', '    = 5;']}, ['206'], "Despues de L46: 'contador' y '= 5;' en lineas distintas")
v('ID', 'reservada_con_comentario', {46: [L(46), '$if // nombre', '    = 5;']}, ['204'], "Despues de L46: '$if // nombre' y '= 5;' (comentario en medio)")
# '$-notas' se parece a '$notas': si $notas pierde su valor, el 901 se omite a proposito (lo explica el 202)
v('ID', 'elemento_de_arreglo', {50: [L(50), '$-notas[0] = 5;']}, ['202'], "Despues de L50: '$-notas[0] = 5;' (elemento de arreglo)", {'var:$notas'})
v('ID', 'derecha_no_se_valida', {50: [L(50), '$copia = $1abc . $-d;']}, [], "Despues de L50: '$copia = $1abc . $-d;' (a la derecha del = no se valida)")
v('ID', 'nombres_validos', {46: [L(46), '$_x = 1;', '$__ = 2;', '$a1b2 = 3;', '$X = 4;', '$classes = 5;']}, [],
  "Despues de L46: nombres validos ($_x, $__, $a1b2, $X, $classes)")

# =====================================================================================
# ET  Etiquetas <?php y ?> (R4 del Proyecto 1)
# =====================================================================================
v('ET', 'apertura_espacio_antes', {1: ' <?php'}, ['301'], "L1: ' <?php' (espacio antes)", {'etiqueta'})
v('ET', 'apertura_tab_antes', {1: '\t<?php'}, ['301'], "L1: tabulacion antes de <?php", {'etiqueta'})
v('ET', 'apertura_linea_vacia_antes', {1: ['', '<?php']}, ['301'], "Linea en blanco antes de <?php", {'etiqueta'})
v('ET', 'apertura_texto_antes', {1: ['Hola', '<?php']}, ['301'], "Texto 'Hola' antes de <?php", {'etiqueta'})
v('ET', 'apertura_comentario_antes', {1: ['// inicio', '<?php']}, ['301'], "Linea '// inicio' antes de <?php", {'etiqueta'})
for mal in ['<? php', '<?PHP', '<?Php', '<?pHp', '<php', '<PHP']:
    v('ET', 'apertura_' + re.sub(r'\W', '', mal.replace(' ', '_espacio_')).lower() or 'x', {1: mal}, ['302'],
      f"L1: '{mal}' (apertura mal escrita)", {'etiqueta'})
v('ET', 'apertura_falta', {1: None}, ['303'], "Se borra la L1 (<?php)", {'etiqueta'})
v('ET', 'apertura_en_linea_2', {1: ['', '<?php']}, ['301'], "<?php en la linea 2", {'etiqueta'})
v('ET', 'apertura_con_codigo_valido', {1: '<?php echo "";'}, [], "L1: '<?php echo \"\";' (codigo en la misma linea: valido)", {'etiqueta'})
for mal in ['? >', '?  >']:
    v('ET', 'cierre_' + mal.replace(' ', '_'), {53: mal}, ['304'], f"L53: '{mal}' (cierre mal escrito)", {'etiqueta'})
v('ET', 'cierre_falta', {53: None}, ['305'], "Se borra la L53 (?>)", {'etiqueta'})
v('ET', 'cierre_codigo_despues', {53: '?> echo "x";'}, ['306'], "L53: '?> echo \"x\";'", {'etiqueta'})
v('ET', 'cierre_texto_pegado', {53: '?>fin'}, ['306'], "L53: '?>fin'", {'etiqueta'})
v('ET', 'cierre_linea_con_codigo_despues', {53: ['?>', 'echo "x";']}, ['307'], "Despues de ?> una linea con codigo", {'etiqueta'})
v('ET', 'cierre_html_despues', {53: ['?>', '<p>hola</p>']}, ['307'], "Despues de ?> una linea con HTML", {'etiqueta'})
v('ET', 'cierre_espacios_despues', {53: '?>   '}, ['308'], "L53: '?>' seguido de espacios", {'etiqueta'})
v('ET', 'cierre_salto_final', {53: ['?>', '']}, ['308'], "Salto de linea despues de ?>", {'etiqueta'})
v('ET', 'cierre_lineas_en_blanco', {53: ['?>', '', '']}, ['308'], "Lineas en blanco despues de ?>", {'etiqueta'})
v('ET', 'cierre_con_codigo_antes_valido', {52: None, 53: 'echo ""; ?>'}, [], "L53: 'echo \"\"; ?>' (valido)", {'etiqueta'})
v('ET', 'sin_ninguna_etiqueta', {1: None, 53: None}, ['303', '305'], "Se borran <?php y ?>", {'etiqueta'})

# =====================================================================================
# CO  Comentarios (R5 del Proyecto 1)
# =====================================================================================
COMENTARIOS = [2, 8, 11, 15, 24, 27, 34, 42, 46]
for n in COMENTARIOS:
    v('CO', f'L{n:02d}_una_barra', {n: L(n).replace('//', '/', 1)}, ['401'], f"L{n}: '//' -> '/'")
    v('CO', f'L{n:02d}_sin_marca', {n: L(n).replace('// ', '', 1)}, ['402'], f"L{n}: se quita '// ' (texto suelto)")
    v('CO', f'L{n:02d}_numeral_valido', {n: L(n).replace('//', '#', 1)}, [], f"L{n}: '//' -> '#' (valido)")
    v('CO', f'L{n:02d}_triple_barra_valido', {n: L(n).replace('//', '///', 1)}, [], f"L{n}: '//' -> '///' (valido)")
    v('CO', f'L{n:02d}_continua_sin_marca', {n: [L(n), 'continua el comentario']}, ['402'], f"Despues de L{n}: texto sin // (comentario que sigue)")
v('CO', 'L04_una_barra', {4: L(4).replace('#', '/', 1)}, ['401'], "L4: '#' -> '/'")
v('CO', 'L04_sin_marca', {4: L(4).replace('# ', '', 1)}, ['402'], "L4: se quita '# '")
v('CO', 'L04_pegado_valido', {4: '#Datos del estudiante'}, [], "L4: '#Datos' (valido)")
v('CO', 'bloque_c_no_reconocido', {46: [L(46), '/* comentario de bloque */']}, ['401'], "Despues de L46: '/* ... */' (solo se reconocen // y #)")
v('CO', 'al_final_de_codigo_valido', {47: '$total = 100; // cien', 48: '$Total = 200; # doscientos'}, [], "L47 y L48 con comentario al final (valido)")
v('CO', 'con_palabras_clave_valido', {2: '// if while echo $x = ; { ( "'}, [], "L2: comentario con palabras clave y simbolos (valido)")
v('CO', 'dentro_de_cadena_valido', {44: 'echo "http://uned.ac.cr # no es comentario";'}, [], "L44: '//' y '#' dentro de una cadena (valido)")
v('CO', 'division_en_dos_lineas_valido', {22: ['$promedio = $suma_notas', '    / $cantidad_notas;']}, [], "L22 en dos lineas: '/' al inicio es division (valido)")

# =====================================================================================
# WH  Comando while (R3 del Proyecto 2): lineas 37 a 40
# =====================================================================================
W = L(37)
v('WH', 'sin_parentesis_apertura', {37: W.replace('(', '', 1)}, ['601'], "L37: sin '('", {'bloque:while'})
v('WH', 'sin_parentesis_cierre', {37: W.replace(')', '', 1)}, ['602'], "L37: sin ')'", {'bloque:while'})
v('WH', 'sin_parentesis', {37: W.replace('(', '', 1).replace(')', '', 1)}, ['601', '602'], "L37: sin '(' ni ')'", {'bloque:while'})
v('WH', 'condicion_vacia', {37: 'while () {'}, ['603'], "L37: 'while () {'", {'bloque:while'})
v('WH', 'condicion_con_espacios', {37: 'while (   ) {'}, ['603'], "L37: 'while (   ) {'", {'bloque:while'})
v('WH', 'sin_llave_apertura', {37: W[:-2]}, ['604'], "L37: sin '{'", {'bloque:while'})
v('WH', 'sin_llave_cierre', {40: None}, ['605'], "Se borra la L40 ('}')", {'bloque:while'})
v('WH', 'bloque_vacio', {38: None, 39: None}, ['606'], "Se borran L38 y L39", {'bloque:while'})
v('WH', 'bloque_solo_comentario', {38: '    // pendiente', 39: None}, ['606'], "Bloque con solo un comentario", {'bloque:while'})
v('WH', 'bloque_lineas_en_blanco', {38: '', 39: ''}, ['606'], "Bloque con solo lineas en blanco", {'bloque:while'})
for n in (38, 39):
    for k in (0, 1, 2, 3, 5, 6, 7, 8):
        v('WH', f'L{n}_{k}_espacios', {n: ' ' * k + L(n).strip()}, ['607'], f"L{n} con {k} espacios", {'bloque:while'})
    v('WH', f'L{n}_tabulacion', {n: '\t' + L(n).strip()}, ['608'], f"L{n} indentada con tabulacion", {'bloque:while'})
    v('WH', f'L{n}_tab_y_espacios', {n: '\t    ' + L(n).strip()}, ['608'], f"L{n} con tabulacion y espacios", {'bloque:while'})
for k in (1, 2, 3, 4, 5, 8):
    v('WH', f'cierre_{k}_espacios', {40: ' ' * k + '}'}, ['609'], f"L40: '}}' con {k} espacios", {'bloque:while'})
v('WH', 'cierre_tabulacion', {40: '\t}'}, ['608'], "L40: '}' con tabulacion", {'bloque:while'})
v('WH', 'llave_sola_alineada_valido', {37: [W[:-2], '{']}, [], "La '{' sola en la linea siguiente, alineada (valido)", {'bloque:while'})
for k in (1, 2, 4, 8):
    v('WH', f'llave_sola_{k}_espacios', {37: [W[:-2], ' ' * k + '{']}, ['610'], f"La '{{' sola en la linea siguiente con {k} espacios", {'bloque:while'})
v('WH', 'llave_sola_tabulacion', {37: [W[:-2], '\t{']}, ['608'], "La '{' sola en la linea siguiente con tabulacion", {'bloque:while'})
for mal in ('whlie', 'wile', 'whiel', 'whille', 'wihle', 'hwile'):
    v('WH', f'mal_escrito_{mal}', {37: W.replace('while', mal)}, ['504'], f"L37: '{mal}' en lugar de while", {'bloque:while'})
v('WH', 'mal_escrito_mayusculas', {37: W.replace('while', 'WHLIE')}, ['504'], "L37: 'WHLIE' (mayusculas y mal escrito)", {'bloque:while'})
v('WH', 'mal_escrito_sin_parentesis_cierre', {37: W.replace('whil', 'whli').replace(') {', ' {')}, ['504', '602'], "L37: 'whlie (... {' (mal escrito y sin ')')", {'bloque:while'})
v('WH', 'mal_escrito_sin_parentesis_apertura', {37: W.replace('while (', 'whlie ')}, ['504', '601'], "L37: 'whlie ...) {' (mal escrito y sin '(')", {'bloque:while'})
v('WH', 'mal_escrito_sin_llave', {37: W.replace('whil', 'whli')[:-2]}, ['504', '604'], "L37: 'whlie (...)' (mal escrito y sin '{')", {'bloque:while'})
v('WH', 'mal_escrito_condicion_vacia', {37: 'whlie () {'}, ['504', '603'], "L37: 'whlie () {' (mal escrito y sin condicion)", {'bloque:while'})
v('WH', 'mal_escrito_cuerpo_mal_indentado', {37: W.replace('whil', 'whli'), 38: '  ' + L(38).strip()}, ['504', '607'], "L37 'whlie' y L38 con 2 espacios (se revisa como while)", {'bloque:while'})
v('WH', 'mal_escrito_tras_sentencia_sin_punto_y_coma', {35: '$contador_revision = 0', 37: W.replace('whil', 'whli')}, ['504'], "L35 sin ';' y L37 'whlie': la sentencia no se traga la estructura", {'bloque:while', 'var:$contador_revision'})
v('WH', 'mayusculas_valido', {37: W.replace('while', 'WHILE')}, [], "L37: 'WHILE' (valido)", {'bloque:while'})
v('WH', 'sin_espacios_valido', {37: W.replace('while (', 'while(').replace(') {', '){')}, [], "L37: 'while(...){' (valido)", {'bloque:while'})
v('WH', 'comentario_tras_llave_valido', {37: W + ' // ciclo', 40: '} # fin'}, [], "Comentarios despues de '{' y de '}' (valido)", {'bloque:while'})
v('WH', 'condicion_en_dos_lineas_valido', {37: ['while ($contador_revision <', '        $cantidad_notas) {']}, [], "Condicion en dos lineas (valido)", {'bloque:while'})
v('WH', 'condicion_con_parentesis_valido', {37: 'while (($contador_revision < $cantidad_notas) && ($cantidad_notas > 0)) {'}, [], "Condicion con parentesis internos (valido)", {'bloque:while'})
v('WH', 'lineas_en_blanco_dentro_valido', {38: ['', L(38), '']}, [], "Lineas en blanco dentro del bloque (valido)", {'bloque:while'})
v('WH', 'if_anidado_valido', {38: [L(38), '    if ($contador_revision == 2) {', '        echo "Mitad\\n";', '    }']}, [], "Un if anidado con 8 espacios (valido)", {'bloque:while'})
v('WH', 'if_anidado_mal_indentado', {38: [L(38), '    if ($contador_revision == 2) {', '      echo "Mitad\\n";', '    }']}, ['707'], "If anidado con 6 espacios en su contenido", {'bloque:while'})

# =====================================================================================
# IF  Comando if y bloque else (R4 del Proyecto 2): lineas 28 a 32
# =====================================================================================
I = L(28)
v('IF', 'sin_parentesis_apertura', {28: I.replace('(', '', 1)}, ['701'], "L28: sin '('", {'bloque:if'})
v('IF', 'sin_parentesis_cierre', {28: I.replace(')', '', 1)}, ['702'], "L28: sin ')'", {'bloque:if'})
v('IF', 'sin_parentesis', {28: I.replace('(', '', 1).replace(')', '', 1)}, ['701', '702'], "L28: sin '(' ni ')'", {'bloque:if'})
v('IF', 'condicion_vacia', {28: 'if () {'}, ['703'], "L28: 'if () {'", {'bloque:if'})
v('IF', 'sin_llave_apertura', {28: I[:-2]}, ['704'], "L28: sin '{'", {'bloque:if'})
v('IF', 'sin_llave_cierre', {30: 'else {'}, ['705'], "L30: 'else {' (falta la '}' del if)", {'bloque:if'})
v('IF', 'bloque_vacio', {29: None}, ['706'], "Se borra la L29", {'bloque:if'})
v('IF', 'bloque_solo_comentario', {29: '    // pendiente'}, ['706'], "Bloque del if con solo un comentario", {'bloque:if'})
for k in (0, 1, 2, 3, 5, 6, 8):
    v('IF', f'L29_{k}_espacios', {29: ' ' * k + L(29).strip()}, ['707'], f"L29 con {k} espacios", {'bloque:if'})
v('IF', 'L29_tabulacion', {29: '\t' + L(29).strip()}, ['708'], "L29 con tabulacion", {'bloque:if'})
for k in (1, 2, 4):
    v('IF', f'cierre_{k}_espacios', {30: ' ' * k + '} else {'}, ['709'], f"L30: la '}}' del if con {k} espacios", {'bloque:if'})
v('IF', 'llave_sola_alineada_valido', {28: [I[:-2], '{']}, [], "La '{' del if sola en la linea siguiente, alineada (valido)", {'bloque:if'})
for k in (2, 4):
    v('IF', f'llave_sola_{k}_espacios', {28: [I[:-2], ' ' * k + '{']}, ['710'], f"La '{{' del if sola con {k} espacios", {'bloque:if'})
v('IF', 'else_sin_llave_apertura', {30: '} else'}, ['711'], "L30: '} else' sin '{'", {'bloque:if'})
v('IF', 'else_sin_llave_cierre', {32: None}, ['712'], "Se borra la L32 (la '}' del else)", {'bloque:if'})
v('IF', 'else_vacio', {31: None}, ['713'], "Se borra la L31", {'bloque:if'})
v('IF', 'else_solo_comentario', {31: '    # nada'}, ['713'], "Bloque del else con solo un comentario", {'bloque:if'})
v('IF', 'else_sin_if', {46: [L(46), 'else {', '    echo "x";', '}']}, ['714'], "Despues de L46: 'else { ... }' sin if", {'bloque:if'})
for k in (0, 2, 6, 8):
    v('IF', f'L31_{k}_espacios', {31: ' ' * k + L(31).strip()}, ['715'], f"L31 (bloque del else) con {k} espacios", {'bloque:if'})
v('IF', 'L31_tabulacion', {31: '\t' + L(31).strip()}, ['716'], "L31 con tabulacion", {'bloque:if'})
for k in (1, 2, 4):
    v('IF', f'cierre_else_{k}_espacios', {32: ' ' * k + '}'}, ['717'], f"L32: '}}' del else con {k} espacios", {'bloque:if'})
v('IF', 'else_en_su_linea_valido', {30: ['}', 'else {']}, [], "'}' y 'else {' en lineas distintas, alineados (valido)", {'bloque:if'})
for k in (2, 4):
    v('IF', f'else_en_su_linea_{k}_espacios', {30: ['}', ' ' * k + 'else {']}, ['718'], f"'else' en su propia linea con {k} espacios", {'bloque:if'})
v('IF', 'else_llave_sola_valido', {30: ['} else', '{']}, [], "La '{' del else sola en la linea siguiente (valido)", {'bloque:if'})
v('IF', 'else_llave_sola_4_espacios', {30: ['} else', '    {']}, ['719'], "La '{' del else sola con 4 espacios", {'bloque:if'})
v('IF', 'estilo_allman_valido', {28: [I[:-2], '{'], 30: ['}', 'else', '{']}, [], "Cada llave y el else en su propia linea (valido)", {'bloque:if'})
for mal, sug in (('iff', 'if'), ('fi', 'if'), ('ifx', 'if')):
    v('IF', f'mal_escrito_{mal}', {28: I.replace('if', mal, 1)}, ['504'], f"L28: '{mal}' en lugar de if", {'bloque:if'})
v('IF', 'mal_escrito_mayusculas', {28: I.replace('if', 'IFF', 1)}, ['504'], "L28: 'IFF' (mayusculas y mal escrito)", {'bloque:if'})
v('IF', 'mal_escrito_sin_parentesis_cierre', {28: I.replace('if', 'iff', 1).replace(') {', ' {')}, ['504', '702'], "L28: 'iff (... {' (mal escrito y sin ')')", {'bloque:if'})
v('IF', 'mal_escrito_sin_parentesis', {28: 'iff $promedio >= 70 {'}, ['504', '701', '702'], "L28: 'iff $promedio >= 70 {' (mal escrito y sin parentesis)", {'bloque:if'})
v('IF', 'mal_escrito_sin_llave', {28: I.replace('if', 'iff', 1)[:-2]}, ['504', '704'], "L28: 'iff (...)' (mal escrito y sin '{')", {'bloque:if'})
v('IF', 'mal_escrito_condicion_vacia', {28: 'iff () {'}, ['504', '703'], "L28: 'iff () {' (mal escrito y sin condicion)", {'bloque:if'})
v('IF', 'mal_escrito_cuerpo_mal_indentado', {28: I.replace('if', 'iff', 1), 29: '  ' + L(29).strip()}, ['504', '707'], "L28 'iff' y L29 con 2 espacios (se revisa como if)", {'bloque:if'})
v('IF', 'elseif_mal_escrito', {30: ['} elsif ($promedio >= 60) {', '    echo "Estado: Reposicion\\n";', '} else {']}, ['504'], "Se agrega '} elsif (...) {' en lugar de elseif", {'bloque:if'})
for mal in ('els', 'esle', 'elze', 'eles'):
    v('IF', f'else_mal_escrito_{mal}', {30: '} ' + mal + ' {'}, ['504'], f"L30: '}} {mal} {{' en lugar de else", {'bloque:if'})
for forma in ('}else{', '} else{', '}else {', '} ELSE {'):
    v('IF', 'else_' + re.sub(r'\W', '', forma.replace(' ', '_')).lower() + '_valido', {30: forma}, [], f"L30: '{forma}' (valido)", {'bloque:if'})
v('IF', 'sin_espacios_valido', {28: 'if($promedio >= 70){'}, [], "L28: 'if(...){' (valido)", {'bloque:if'})
v('IF', 'mayusculas_valido', {28: I.replace('if', 'IF', 1)}, [], "L28: 'IF' (valido)", {'bloque:if'})
v('IF', 'elseif_valido', {30: ['} elseif ($promedio >= 60) {', '    echo "Estado: Reposicion\\n";', '} else {']}, [], "Se agrega un elseif (valido)", {'bloque:if'})
v('IF', 'else_if_valido', {30: ['} else if ($promedio >= 60) {', '    echo "Estado: Reposicion\\n";', '} else {']}, [], "Se agrega un 'else if' (valido)", {'bloque:if'})
v('IF', 'elseif_sin_parentesis', {30: ['} elseif $promedio >= 60 {', '    echo "Estado: Reposicion\\n";', '} else {']}, ['701', '702'], "elseif sin parentesis", {'bloque:if'})
v('IF', 'elseif_vacio', {30: ['} elseif ($promedio >= 60) {', '} else {']}, ['706'], "elseif con el bloque vacio", {'bloque:if'})
v('IF', 'condicion_con_asignacion_valido', {28: I.replace('>=', '=')}, [], "L28: 'if ($promedio = 70)' (la condicion se considera correcta)", {'bloque:if'})

# =====================================================================================
# EC  Comando echo (R5 del Proyecto 2): las 8 lineas con echo
# =====================================================================================
for n in (12, 13, 25, 29, 31, 38, 44, 51):
    t = L(n)
    s = ind(n)
    expr = t.strip()[5:-1]
    # las variables que usa la linea: si el echo se reemplaza, ese uso desaparece
    eti = {f'echo:{n}'} | {f'var:{x.lower()}' for x in re.findall(r'\$\w+', t)} \
        | ({'bloque:if'} if n in (29, 31) else {'bloque:while'} if n == 38 else set())
    v('EC', f'L{n}_sin_punto_y_coma', {n: t[:-1]}, ['805'], f"L{n}: sin ';'", eti)
    v('EC', f'L{n}_parentesis_agrupan', {n: f'{s}echo ({expr});'}, ['802'], f"L{n}: 'echo (...);'", eti)
    v('EC', f'L{n}_funcion', {n: f'{s}echo strtoupper({expr});'}, ['802'], f"L{n}: 'echo strtoupper(...);' (tampoco en funciones)", eti)
    v('EC', f'L{n}_sin_expresion', {n: f'{s}echo;'}, ['801'], f"L{n}: 'echo;'", eti)
    v('EC', f'L{n}_comilla_simple_abierta', {n: f"{s}echo 'Texto;"}, ['804'], f"L{n}: \"echo 'Texto;\"", eti)
    v('EC', f'L{n}_operador_sin_valor', {n: f'{s}echo {expr} . ;'}, ['806'], f"L{n}: operador '.' sin valor a la derecha", eti)
    v('EC', f'L{n}_falta_operador', {n: f'{s}echo "Dato: " {expr};'}, ['806'], f"L{n}: falta el '.' entre dos valores", eti)
    v('EC', f'L{n}_mal_escrito', {n: f'{s}eco {expr};'}, ['504'], f"L{n}: 'eco' en lugar de echo", eti)
    v('EC', f'L{n}_mayusculas_valido', {n: f'{s}ECHO {expr};'}, [], f"L{n}: 'ECHO' (valido)", eti)
    v('EC', f'L{n}_parentesis_en_cadena_valido', {n: f'{s}echo "(nota)" . {expr};'}, [], f"L{n}: parentesis dentro de una cadena (valido)", eti)
    if '"' in expr:
        j = expr.rindex('"')
        v('EC', f'L{n}_comilla_doble_abierta', {n: f'{s}echo {expr[:j]}{expr[j + 1:]};'}, ['803'], f"L{n}: falta la comilla doble de cierre", eti)
    if ' . ' in expr:
        a, b = expr.split(' . ', 1)
        v('EC', f'L{n}_en_dos_lineas_valido', {n: [f'{s}echo {a}', f'{s}    . {b};']}, [], f"L{n} en dos lineas (valido)", eti)

# =====================================================================================
# SE  Analisis semantico: 901 (variable sin valor) y 902 (division entre el numero 0)
# =====================================================================================
USOS = [(12, '$nombre_estudiante'), (13, '$cantidad_notas'), (18, '$cantidad_notas'), (19, '$notas'),
        (22, '$suma_notas'), (25, '$promedio'), (28, '$promedio'), (37, '$contador_revision'),
        (38, '$notas'), (39, '$contador_revision'), (44, '$mensaje_final'), (50, '$total'), (51, '$resultado')]
for n, var in USOS:
    t = L(n)
    k = t.rindex(var)
    otra = var[0] + (var[1].upper() if var[1].islower() else var[1].lower()) + var[2:]
    eti = {f'var:{var.lower()}'} | ({'bloque:if'} if n == 28 else {'bloque:while'} if n in (37, 38, 39) else set())
    if otra not in ASIGNACIONES.values():  # '$total' -> '$Total' ya existe: no es un error
        v('SE', f'L{n:02d}_mayuscula', {n: t[:k] + otra + t[k + len(var):]}, ['901'], f"L{n}: '{var}' -> '{otra}'", eti)
    letra = var[-2] + var[-1]
    typo = var[:-2] + letra[::-1] if letra[0] != letra[1] else var + 'x'
    v('SE', f'L{n:02d}_letras_cambiadas', {n: t[:k] + typo + t[k + len(var):]}, ['901'], f"L{n}: '{var}' -> '{typo}'", eti)
v('SE', 'uso_antes_de_asignar', {46: [L(46), 'echo $total;']}, ['901'], "Despues de L46: 'echo $total;' (se asigna en L47)")
v('SE', 'variable_inventada', {44: 'echo $mensaje_final . $despedida;'}, ['901'], "L44: '$despedida' nunca recibe un valor")
v('SE', 'variable_en_cadena', {25: 'echo "Promedio: $promedio_final\\n";'}, ['901'], "L25: '$promedio_final' dentro de la cadena")
v('SE', 'funcion_anonima_valido', {46: [L(46), '$ordenadas = $notas;', 'usort($ordenadas, function ($a, $b) {', '    return $a - $b;', '});']}, [], "Despues de L46: usort con function ($a, $b) (los parametros tienen valor)")
v('SE', 'funcion_flecha_valido', {46: [L(46), '$dobles = array_map(fn($n) => $n * 2, $notas);']}, [], "Despues de L46: fn($n) => $n * 2 (el parametro tiene valor)")
v('SE', 'funcion_anonima_use_sin_valor', {46: [L(46), '$f = function ($a) use ($bono) {', '    return $a + $bono;', '};']}, ['901'], "Despues de L46: function ($a) use ($bono): $bono no tiene valor")
v('EC', 'eco_tras_sentencia_sin_punto_y_coma', {43: L(43)[:-1], 44: 'eco $mensaje_final;'}, ['504'], "L43 sin ';' y L44 'eco': la sentencia no se traga el echo", {'echo:44', 'var:$mensaje_final'})
v('SE', 'division_entre_cero', {22: L(22).replace('$cantidad_notas;', '0;')}, ['902'], "L22: '/ 0'")
v('SE', 'division_entre_cero_decimal', {22: L(22).replace('$cantidad_notas;', '0.0;')}, ['902'], "L22: '/ 0.0'")
v('SE', 'modulo_entre_cero', {22: L(22).replace('/ $cantidad_notas;', '% 0;')}, ['902'], "L22: '% 0'")
v('SE', 'division_asignada_entre_cero', {50: [L(50), '$resultado /= 0;']}, ['902'], "Despues de L50: '$resultado /= 0;'")
v('SE', 'division_entre_cero_en_echo', {44: 'echo 100 / 0;'}, ['902'], "L44: 'echo 100 / 0;'")
v('SE', 'division_decimal_valido', {22: L(22).replace('$cantidad_notas;', '0.5;')}, [], "L22: '/ 0.5' (valido)")
v('SE', 'division_en_cadena_valido', {44: 'echo "100 / 0";'}, [], "L44: '/ 0' dentro de una cadena (valido)")
v('SE', 'division_entre_variable_valido', {22: L(22).replace('$cantidad_notas;', '$cantidad_notas - 0;')}, [], "L22: '/ $cantidad_notas - 0' (valido)")

# =====================================================================================
# FO  El for (no se exige, aclaracion de la profesora): bloque generico
# =====================================================================================
F = L(18)
v('FO', 'cuerpo_2_espacios_valido', {19: '  ' + L(19).strip()}, [], "L19 con 2 espacios (el for no se valida)", {'bloque:for'})
v('FO', 'cuerpo_tabulacion_valido', {19: '\t' + L(19).strip()}, [], "L19 con tabulacion (el for no se valida)", {'bloque:for'})
v('FO', 'cierre_desalineado_valido', {20: '  }'}, [], "L20: '  }' (el for no se valida)", {'bloque:for'})
v('FO', 'llave_abajo_valido', {18: [F[:-2], '{']}, [], "La '{' del for en la linea siguiente (valido)", {'bloque:for'})
v('FO', 'cuerpo_vacio_valido', {19: '    // nada'}, [], "Bloque del for con solo un comentario (el for no se valida)", {'bloque:for'})
v('FO', 'sin_llave_cierre', {20: None}, ['505'], "Se borra la L20: el bloque del for no cierra", {'bloque:for'})
v('FO', 'sin_llave_apertura', {18: F[:-2]}, ['503'], "L18 sin '{': la '}' de L20 queda sin apertura", {'bloque:for'})
v('FO', 'mal_escrito', {18: F.replace('for', 'fro', 1)}, ['504'], "L18: 'fro' en lugar de for", {'bloque:for'})
v('FO', 'mal_escrito_sin_parentesis_cierre', {18: F.replace('for', 'fro', 1).replace(') {', ' {')}, ['504'], "L18: 'fro (...; ...; ... {' (el for no se valida: solo el 504)", {'bloque:for'})
v('FO', 'comas_en_cabecera_valido', {18: F.replace('$i = 0;', '$i = 0, $j = 0;')}, [], "Cabecera con comas (valido)", {'bloque:for'})

# =====================================================================================
# FA  Formato del archivo completo (saltos de linea, codificacion, estilos)
# =====================================================================================
texto = '\r\n'.join(B)
v('FA', 'saltos_lf', None, [], "Todo el archivo con saltos LF (Linux)", {'archivo'}, texto.replace('\r\n', '\n').encode())
v('FA', 'saltos_cr', None, [], "Todo el archivo con saltos CR", {'archivo'}, texto.replace('\r\n', '\r').encode())
mixto = ''.join(l + ('\r\n', '\n', '\r')[i % 3] for i, l in enumerate(B[:-1])) + B[-1]
v('FA', 'saltos_mezclados', None, [], "Saltos CRLF, LF y CR mezclados", {'archivo'}, mixto.encode())
v('FA', 'bom_utf8', None, ['301'], "Guardado como 'UTF-8 con BOM'", {'archivo'}, b'\xef\xbb\xbf' + texto.encode())
v('FA', 'ansi_windows_1252', None, [], "Guardado como ANSI (Windows-1252)", {'archivo'}, texto.encode('cp1252'))
v('FA', 'espacios_al_final_de_lineas', None, [], "Espacios al final de cada linea (salvo la de ?>)", {'archivo'},
  '\r\n'.join([l + '  ' for l in B[:-1]] + [B[-1]]).encode())
v('FA', 'tabulaciones_en_bloques', None, ['708', '716', '608', '608'], "Tabulaciones en lugar de espacios en todos los bloques", {'archivo'},
  '\r\n'.join(l.replace('    ', '\t', 1) if l.startswith('    ') else l for l in B).encode())
v('FA', 'dos_espacios_en_bloques', None, ['707', '715', '607', '607'], "2 espacios en lugar de 4 en todos los bloques", {'archivo'},
  '\r\n'.join(l.replace('    ', '  ', 1) if l.startswith('    ') else l for l in B).encode())
allman = []
for l in B:
    if l.endswith(' {') and not l.startswith('}'):
        allman += [l[:-2], '{']
    elif l == '} else {':
        allman += ['}', 'else', '{']
    else:
        allman.append(l)
v('FA', 'estilo_allman_valido', None, [], "Cada '{' y el else en su propia linea (valido)", {'archivo'}, '\r\n'.join(allman).encode())
mayus = [re.sub(r'\b(echo|if|else|while|for)\b', lambda m: m.group(1).upper(), l) if not l.lstrip().startswith(('//', '#')) else l for l in B]
v('FA', 'palabras_clave_mayusculas_valido', None, [], "Palabras clave en mayusculas (valido)", {'archivo'}, '\r\n'.join(mayus).encode())
v('FA', 'archivo_vacio', None, ['303', '305'], "Archivo vacio", {'archivo'}, b'')
v('FA', 'solo_etiquetas', None, [], "Solo '<?php' y '?>'", {'archivo'}, b'<?php\r\n?>')

# =====================================================================================
# Combinaciones: variantes compatibles (lineas separadas y sin interaccion por diseno)
# =====================================================================================
SIMPLES = [x for x in VARIANTES if x.contenido is None and x.esperados and 'etiqueta' not in x.etiquetas]


def zona(c):
    return {k for n in c for k in (n - 1, n, n + 1)}


def compatibles(grupo):
    zonas = [zona(x.cambios) for x in grupo]
    if any(zonas[i] & zonas[j] for i in range(len(grupo)) for j in range(i + 1, len(grupo))):
        return False
    etiquetas = [x.etiquetas for x in grupo]
    for i in range(len(grupo)):
        for j in range(i + 1, len(grupo)):
            comunes = etiquetas[i] & etiquetas[j]
            # misma variable o mismo bloque: sus errores interactuan (por ejemplo, el 901 se omite
            # si la asignacion ya tiene error lexico, o un bloque sin llave cambia la indentacion)
            if comunes:
                return False
    return True


def combinar(rnd, por_caso, cantidad, prefijo):
    hechos, intentos = [], 0
    while len(hechos) < cantidad and intentos < cantidad * 200:
        intentos += 1
        grupo = rnd.sample(SIMPLES, por_caso)
        if not compatibles(grupo):
            continue
        cambios, esperados = {}, []
        for x in grupo:
            cambios.update(x.cambios)
            esperados += x.esperados
        nombre = f'{prefijo}{len(hechos) + 1:03d}_' + '__'.join(x.nombre.split('_', 1)[0] for x in grupo)
        hechos.append(Variante(nombre, cambios, None, esperados,
                               ' + '.join(x.descripcion for x in grupo), set()))
    return hechos


# =====================================================================================
# Ejecucion
# =====================================================================================
def contenido(x):
    return x.contenido if x.contenido is not None else aplicar(x.cambios)


def ejecutar(jar, carpeta, variantes):
    shutil.rmtree(carpeta, ignore_errors=True)
    os.makedirs(carpeta)
    shutil.copy(jar, carpeta)
    for x in variantes:
        open(os.path.join(carpeta, x.nombre + '.php'), 'wb').write(contenido(x))
    r = subprocess.run(['java', '-jar', os.path.basename(jar)], cwd=carpeta, capture_output=True, text=True, errors='replace')
    fallos = [l for l in r.stdout.splitlines() if 'se detuvo' in l or 'Exception' in l]
    diferentes = []
    for x in variantes:
        ruta = os.path.join(carpeta, x.nombre + '-errores.log')
        if not os.path.exists(ruta):
            diferentes.append((x, 'SIN LOG'))
            continue
        obtenidos = re.findall(r'(?:^|[\r\n])Error (\d{3})\.', open(ruta, 'rb').read().decode('utf-8', 'replace'))
        if collections.Counter(obtenidos) != collections.Counter(x.esperados):
            diferentes.append((x, sorted(obtenidos)))
    return diferentes, fallos


def tabla(variantes):
    filas = ['VARIANTES DEL CODIGO DE LA CATEDRA (recursos/CodigoProyecto.php)',
             'Cada archivo de "casos" es una copia del codigo de la catedra con un cambio (o una combinacion',
             'de cambios) y "esperados" tiene el -errores.log exacto que debe generar. "-" = sin errores.',
             'Las lineas (L28, L37...) son las del codigo original.', '',
             f'{"Archivo":<48}{"Errores esperados":<24}Cambio']
    for x in variantes:
        cod = ' '.join(sorted(x.esperados)) or '-'
        nombre = x.nombre + '.php'
        if len(nombre) >= 48:
            filas += [nombre, ' ' * 48 + f'{cod:<24}{x.descripcion}']
        else:
            filas.append(f'{nombre:<48}{cod:<24}{x.descripcion}')
    return '\r\n'.join(filas) + '\r\n'


def main():
    jar = os.path.abspath(sys.argv[1])
    if sys.argv[2] == '--carpeta':
        destino = os.path.abspath(sys.argv[3])
        rnd = random.Random(2026)
        todas = VARIANTES + combinar(rnd, 2, 60, 'CB') + combinar(rnd, 3, 40, 'CC')
        trabajo = destino + '.tmp'
        diferentes, fallos = ejecutar(jar, trabajo, todas)
        for x, obtenidos in diferentes:
            print(f'DIFERENTE {x.nombre}: esperado {sorted(x.esperados)} obtenido {obtenidos}  ({x.descripcion})')
        print(f'Variantes: {len(todas)}  correctas: {len(todas) - len(diferentes)}  diferentes: {len(diferentes)}  fallos internos: {len(fallos)}')
        if diferentes or fallos:
            sys.exit(1)
        # solo se reemplazan casos/ y esperados/ (los scripts y el LEEME de la carpeta se conservan)
        shutil.rmtree(os.path.join(destino, 'casos'), ignore_errors=True)
        shutil.rmtree(os.path.join(destino, 'esperados'), ignore_errors=True)
        os.makedirs(os.path.join(destino, 'casos'))
        os.makedirs(os.path.join(destino, 'esperados'))
        for x in todas:
            shutil.copy(os.path.join(trabajo, x.nombre + '.php'), os.path.join(destino, 'casos'))
            shutil.copy(os.path.join(trabajo, x.nombre + '-errores.log'), os.path.join(destino, 'esperados'))
        open(os.path.join(destino, 'esperado.txt'), 'wb').write(tabla(todas).encode('utf-8'))
        shutil.rmtree(trabajo)
    elif sys.argv[2] == '--masivo':
        cantidad, por_caso, semilla, trabajo = int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]), sys.argv[6]
        todas = combinar(random.Random(semilla), por_caso, cantidad, 'M')
        diferentes, fallos = ejecutar(jar, trabajo, todas)
        for x, obtenidos in diferentes[:30]:
            print(f'DIFERENTE {x.nombre}: esperado {sorted(x.esperados)} obtenido {obtenidos}  ({x.descripcion})')
        print(f'Combinaciones de {por_caso} errores: {len(todas)}  correctas: {len(todas) - len(diferentes)}  '
              f'diferentes: {len(diferentes)}  fallos internos: {len(fallos)}')
    elif sys.argv[2] == '--individuales':
        diferentes, fallos = ejecutar(jar, sys.argv[3], VARIANTES)
        for x, obtenidos in diferentes:
            print(f'DIFERENTE {x.nombre}: esperado {sorted(x.esperados)} obtenido {obtenidos}  ({x.descripcion})')
        print(f'Variantes individuales: {len(VARIANTES)}  correctas: {len(VARIANTES) - len(diferentes)}  '
              f'diferentes: {len(diferentes)}  fallos internos: {len(fallos)}')


if __name__ == '__main__':
    main()
