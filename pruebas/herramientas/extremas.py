"""Bateria extrema: cada caso declara el multiconjunto EXACTO de errores esperado.
Incluye estructuras anidadas (base_anidada.php), casos limite del Proyecto 1 y 2,
combinaciones de 2 y 3 errores sobre codigo anidado y archivos extremos.

Uso:  python3 extremas.py JAR CARPETA_DE_TRABAJO [filtro_por_nombre]

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import os, re, shutil, subprocess, sys, collections

jar, d = sys.argv[1], sys.argv[2]
filtro = sys.argv[3] if len(sys.argv) > 3 else ''
AQUI = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(AQUI))  # pruebas/herramientas -> raiz del proyecto
CATEDRA = open(os.path.join(REPO, 'recursos', 'CodigoProyecto.php'), 'rb').read().decode('utf-8').split('\r\n')
ANIDADA = open(os.path.join(AQUI, 'base_anidada.php'), 'rb').read().decode('utf-8').split('\r\n')
casos = []  # (nombre, bytes, esperados)


def caso(nombre, texto, esperados, salto='\r\n', codificacion='utf-8'):
    if isinstance(texto, list):
        texto = salto.join(texto)
    casos.append((nombre, texto.encode(codificacion) if isinstance(texto, str) else texto, None if esperados is None else list(esperados)))


def mutar(base, cambios):
    l = list(base)
    for i, v in sorted(cambios.items(), reverse=True):
        if v is None:
            del l[i - 1]
        elif isinstance(v, list):
            l[i - 1:i] = v
        else:
            l[i - 1] = v
    return l


def prog(*lineas):
    """Programa completo con etiquetas, sin errores propios."""
    return ['<?php'] + list(lineas) + ['?>']


C, A = CATEDRA, ANIDADA
def c(n): return C[n - 1]
def a(n): return A[n - 1]

# ======================= A. Estructuras anidadas (programa base_anidada) =====================
caso('A00_base_valida', A, [])
caso('A01_echo_anidado_4esp', mutar(A, {8: '    ' + a(8).strip()}), ['707'])
caso('A02_echo_anidado_tab', mutar(A, {8: '\t\t' + a(8).strip()}), ['708'])
caso('A03_nivel3_8esp', mutar(A, {21: '        ' + a(21).strip()}), ['707'])
caso('A04_nivel3_16esp', mutar(A, {21: ' ' * 16 + a(21).strip()}), ['707'])
caso('A05_cierre_if_nivel2_10esp', mutar(A, {22: ' ' * 10 + '}'}), ['709'])
caso('A06_cierre_while_2esp', mutar(A, {16: '  }'}), ['609'])
caso('A07_cierre_if_antes_de_elseif_2esp', mutar(A, {10: '  ' + a(10).strip()}), ['709'])
caso('A08_else_en_su_linea_alineado', mutar(A, {12: ['    }', '    else {']}), [])
caso('A09_else_en_su_linea_2esp', mutar(A, {12: ['    }', '  else {']}), ['718'])
caso('A10_llave_while_nivel1_sola_alineada', mutar(A, {19: [a(19)[:-2], '    {']}), [])
caso('A11_llave_while_nivel1_sola_8esp', mutar(A, {19: [a(19)[:-2], '        {']}), ['610'])
caso('A12_llave_if_nivel2_sola_alineada', mutar(A, {20: [a(20)[:-2], '        {']}), [])
caso('A13_llave_if_nivel2_sola_4esp', mutar(A, {20: [a(20)[:-2], '    {']}), ['710'])
caso('A14_if_queda_con_una_linea', mutar(A, {9: None}), [])
caso('A15_if_vacio', mutar(A, {8: None, 9: None}), ['706'])
caso('A16_elseif_vacio', mutar(A, {11: None}), ['706'])
caso('A17_else_vacio', mutar(A, {13: None}), ['713'])
caso('A18_if_nivel3_vacio', mutar(A, {21: None}), ['706'])
caso('A19_while_en_for_4esp', mutar(A, {30: '    ' + a(30).strip()}), ['607'])
caso('A20_while_en_for_tab', mutar(A, {30: '\t\t' + a(30).strip()}), ['608'])
caso('A21_while_nivel1_sin_llave', mutar(A, {19: a(19)[:-2]}), ['604'])
caso('A22_while_nivel1_sin_cierre', mutar(A, {24: None}), ['605'])
caso('A23_else_final_sin_cierre', mutar(A, {14: None}), ['712'])
caso('A24_while_sin_parentesis', mutar(A, {6: 'while $i < 3 {'}), ['601', '602'])
caso('A25_echo_sin_pyc', mutar(A, {26: a(26)[:-1]}), ['805'])
caso('A26_echo_parentesis', mutar(A, {33: 'echo ("Total: " . $total . "\\n");'}), ['802'])
caso('A27_echo_anidado_sin_pyc', mutar(A, {8: a(8)[:-1]}), ['805'])
caso('A28_echo_nivel3_comilla', mutar(A, {21: a(21).replace('\\n";', '\\n;')}), ['803'])
caso('A29_whlie_nivel1', mutar(A, {19: a(19).replace('while', 'whlie')}), ['504'])
caso('A30_elseif_sin_parentesis', mutar(A, {10: '    } elseif $notas[$i] >= 70 {'}), ['701', '702'])
caso('A31_else_if_separado', mutar(A, {10: '    } else if ($notas[$i] >= 70) {'}), [])
caso('A32_mayusculas', mutar(A, {6: a(6).replace('while', 'WHILE'), 7: a(7).replace('if', 'If', 1), 12: '    } ELSE {', 33: a(33).replace('echo', 'Echo')}), [])
caso('A33_if_sin_cierre_par_nivel2', mutar(A, {20: '        if ($j == 1 {'}), ['702'])
caso('A34_if_condicion_vacia_nivel2', mutar(A, {20: '        if () {'}), ['703'])
caso('A35_estilo_allman_completo', None, [])  # se arma abajo
caso('A36_uso_sin_valor_anidado', mutar(A, {21: '            echo "Fin " . $Total . "\\n";'}), ['901'])
caso('A37_division_cero_anidada', mutar(A, {9: '        $total = $total + $notas[$i] / 0;'}), ['902'])
caso('A38_dos_errores_misma_linea', mutar(A, {8: '      echo ("Alta")'}), ['707', '802', '805'])

# estilo Allman completo del programa anidado
allman = []
for l in A:
    s = l.rstrip()
    if s.endswith(' {') and not s.lstrip().startswith('}'):
        ind = s[:len(s) - len(s.lstrip())]
        allman += [s[:-2], ind + '{']
    elif s.lstrip().startswith('} ') and s.endswith(' {'):
        ind = s[:len(s) - len(s.lstrip())]
        resto = s.lstrip()[2:-2]
        allman += [ind + '}', ind + resto, ind + '{']
    else:
        allman.append(l)
casos[[n for n, _, _ in casos].index('A35_estilo_allman_completo')] = ('A35_estilo_allman_completo', '\r\n'.join(allman).encode(), [])

# ======================= B. Proyecto 1: identificadores (R3) ================================
caso('B01_guion_en_el_nombre', mutar(C, {5: c(5).replace('$nombre_estudiante', '$nombre-estudiante')}), ['203'])  # el 901 se omite: la asignacion ya tiene error lexico
caso('B02_dolar_solo_con_espacio', mutar(C, {46: [c(46), '$ = 5;']}), ['205'])
caso('B03_letra_con_tilde', mutar(C, {46: [c(46), '$canción = 5;']}), ['203'])
caso('B04_inicia_con_tilde', mutar(C, {46: [c(46), '$ácido = 5;']}), ['202'])
caso('B05_validos_raros', mutar(C, {46: [c(46), '$_x = 1;', '$__ = 2;', '$a1b2 = 3;', '$X = 4;']}), [])
caso('B06_reservada_mayusculas', mutar(C, {46: [c(46), '$WHILE = 1;', '$Return = 2;']}), ['204', '204'])
caso('B07_no_reservada_parecida', mutar(C, {46: [c(46), '$classes = 1;', '$iff = 2;', '$echos = 3;']}), [])
caso('B08_compuestas', mutar(C, {50: [c(50), '$1x += 1;', '$-y .= "a";', '$z@w -= 2;']}), ['201', '202', '203'])
caso('B09_elemento_arreglo', mutar(C, {9: c(9), 10: ['$1notas[0] = 5;', '']}), ['201'])
caso('B10_derecha_no_se_valida', mutar(C, {50: [c(50), '$copia = $1abc;']}), [])  # correccion P1: solo la izquierda
caso('B11_sin_dolar_compuesta', mutar(C, {50: [c(50), 'resultado += 1;']}), ['206'])
caso('B12_comentario_entre_var_e_igual', mutar(C, {46: [c(46), '$if // comentario', '    = 1;']}), ['204'])
caso('B13_asignacion_sin_espacios', mutar(C, {47: '$total=100;'}), [])

# ======================= C. Proyecto 1: etiquetas (R4) ======================================
caso('C01_codigo_en_linea_de_apertura', mutar(C, {1: '<?php echo "x";'}), [])
caso('C02_archivo_vacio', '', ['303', '305'])
caso('C03_solo_comentarios', '// nada\r\n# nada', ['303', '305'])
caso('C04_solo_apertura', '<?php', ['305'])
caso('C05_apertura_y_cierre', '<?php\r\n?>', [])
caso('C06_tab_antes_de_apertura', mutar(C, {1: '\t<?php'}), ['301'])
caso('C07_cierre_con_texto_antes', mutar(C, {53: 'echo "fin"; ?>'}), [])
caso('C08_cierre_seguido_de_html', mutar(C, {53: ['?>', '<p>hola</p>']}), ['307'])
caso('C09_lf_sin_salto_final', '\n'.join(C), [], salto='\n')
caso('C10_cr_solo', '\r'.join(C), [])
caso('C11_apertura_mayuscula_parcial', mutar(C, {1: '<?Php'}), ['302'])
caso('C12_cierre_espacio_y_tab', mutar(C, {53: '?> \t'}), ['308'])
caso('C13_dos_lineas_en_blanco_al_final', '\r\n'.join(C) + '\r\n\r\n', ['308'])

# ======================= D. Proyecto 1: comentarios (R5) ====================================
caso('D01_comentario_al_final_de_codigo', mutar(C, {47: '$total = 100; // cien', 48: '$Total = 200; # doscientos'}), [])
caso('D02_triple_barra', mutar(C, {2: '/// Sistema'}), [])
caso('D03_numeral_pegado', mutar(C, {4: '#Datos'}), [])
caso('D04_url_en_cadena', mutar(C, {44: 'echo "http://uned.ac.cr # no es comentario";'}), [])
caso('D05_comentario_con_palabras_clave', mutar(C, {2: '// if while echo $x = ; {'}), [])
caso('D06_una_barra_con_espacios_antes', mutar(C, {2: '    / Sistema'}), ['401'])
caso('D07_division_en_linea_siguiente', mutar(C, {22: ['$promedio = $suma_notas', '    / $cantidad_notas;']}), [])
caso('D08_texto_suelto_varias_palabras', mutar(C, {8: 'Arreglo de notas del curso'}), ['402'])
caso('D09_comentario_en_bloque', mutar(C, {38: ['    // revisar', c(38)]}), [])
caso('D10_comentario_tras_llave', mutar(C, {37: c(37) + ' // ciclo', 40: '} # fin'}), [])

# ======================= E. Proyecto 2: echo (R5) ===========================================
caso('E01_comillas_escapadas', mutar(C, {44: 'echo "Dijo \\"hola\\"" . \'It\\\'s\';'}), [])
caso('E02_cadena_vacia', mutar(C, {44: 'echo "";'}), [])
caso('E03_pyc_doble', mutar(C, {44: 'echo $mensaje_final;;'}), [])
caso('E04_dos_echo_una_linea', mutar(C, {44: 'echo "a"; echo "b";'}), [])
caso('E05_multilinea_valido', mutar(C, {44: ['echo "a" .', '    "b" .', '    $mensaje_final;']}), [])
caso('E06_multilinea_sin_pyc', mutar(C, {44: ['echo "a" .', '    $mensaje_final']}), ['805'])
caso('E07_dos_cadenas_sin_operador', mutar(C, {44: 'echo "Hola" "Mundo";'}), ['806'])
caso('E08_coma_inicial', mutar(C, {44: 'echo , "a";'}), ['806'])
caso('E09_echo_espacio_pyc', mutar(C, {44: 'echo ;'}), ['801'])
caso('E10_echo_pegado_cadena', mutar(C, {44: 'echo"Fin";'}), [])
caso('E11_echo_mayusculas', mutar(C, {44: 'ECHO $mensaje_final;'}), [])
caso('E12_parentesis_en_cadena', mutar(C, {44: 'echo "(a) [b] {c} ;";'}), [])
caso('E13_funcion', mutar(C, {44: 'echo strlen($mensaje_final);'}), ['802'])
caso('E14_parentesis_y_sin_pyc', mutar(C, {44: 'echo ($mensaje_final)'}), ['802', '805'])
caso('E15_comilla_simple_abierta_en_bloque', mutar(C, {38: "    echo 'Revisando;"}), ['804'])
caso('E16_sin_pyc_antes_de_llave', mutar(C, {39: c(39), 38: c(38)[:-1]}), ['805'])
caso('E17_operador_al_inicio', mutar(C, {44: 'echo . $mensaje_final;'}), ['806'])
caso('E18_eco_mal_escrito', mutar(C, {44: 'eco $mensaje_final;'}), ['504'])
caso('E19_ehco_mal_escrito', mutar(C, {44: 'ehco "Fin";'}), ['504'])
caso('E20_echo_comentario_tras_pyc', mutar(C, {44: 'echo $mensaje_final; // fin'}), [])
caso('E22_cadena_de_5000', mutar(C, {44: 'echo "' + 'x' * 5000 + '";'}), [])
caso('E23_numeros_php', mutar(C, {44: 'echo 1e3 + 2.5E-4 + 0x1F + 0b101 + 0o17 + 1_000;'}), [])
caso('E21_echo_multilinea_con_comentario', mutar(C, {44: ['echo "a" . // primera parte', '    $mensaje_final;']}), [])

# ======================= F. Proyecto 2: while e if (R3, R4) =================================
caso('F01_condicion_con_parentesis_internos', mutar(C, {37: 'while (($contador_revision < $cantidad_notas) && ($cantidad_notas > 0)) {'}), [])
caso('F02_condicion_con_funcion', mutar(C, {37: 'while ($contador_revision < count($notas)) {'}), [])
caso('F03_condicion_espacios', mutar(C, {28: 'if (   ) {'}), ['703'])
caso('F04_sin_espacio_parentesis', mutar(C, {28: 'if($promedio >= 70){', 30: '}else{'}), [])
caso('F05_cierre_par_falta_con_internos', mutar(C, {37: 'while (($contador_revision < $cantidad_notas) {'}), ['602'])
caso('F06_els_mal_escrito', mutar(C, {30: '} els {'}), ['504'])
caso('F07_esle_mal_escrito', mutar(C, {30: '} esle {'}), ['504'])
caso('F08_if_al_final_sin_cierre', mutar(C, {52: ['if ($total > 0) {', '    echo "x";']}), ['705'])
caso('F09_while_vacio_con_lineas_en_blanco', mutar(C, {38: '', 39: ''}), ['606'])
caso('F10_cuerpo_con_if_anidado_4esp', mutar(C, {38: [c(38), '    if ($contador_revision == 1) {', '    echo "uno";', '    }']}), ['707'])
caso('F11_else_if_en_otra_linea_cuerpo_sin_llave', mutar(C, {30: ['} else', '    echo "x";'], 31: None, 32: None}), ['711'])
caso('F12_else_sin_if_tras_while', mutar(C, {40: ['}', 'else {', '    echo "x";', '}']}), ['714'])

# ======================= G. Robustez y archivos extremos ====================================
grande = ['<?php'] + [f'$v{i} = {i};' for i in range(20000)] + ['echo $v19999;', '?>']
caso('G01_veinte_mil_lineas', grande, [])
profundo = ['<?php', '$x = 0;']
for i in range(60):
    profundo.append(' ' * (4 * i) + 'while ($x < 1) {')
profundo.append(' ' * 240 + '$x = 1;')
for i in reversed(range(60)):
    profundo.append(' ' * (4 * i) + '}')
profundo.append('?>')
caso('G02_sesenta_niveles', profundo, [])
caso('G03_linea_muy_larga', ['<?php', 'echo "' + 'x' * 200000 + '";', '?>'], [])
caso('G04_basura_binaria', bytes(range(256)) * 20, None)
caso('G05_solo_bom', b'\xef\xbb\xbf', ['303', '305'])
caso('G06_muchos_errores', ['<?php'] + ['$1x = 1; echo ("a") ' for _ in range(3000)] + ['?>'], None)

# ======================= H. Combinaciones sobre el programa anidado =========================
import random
ESPEC = [  # (lineas que toca, cambios, esperados)
    ({8: '    ' + a(8).strip()}, ['707']), ({16: '  }'}, ['609']), ({21: '        ' + a(21).strip()}, ['707']),
    ({26: a(26)[:-1]}, ['805']), ({33: 'echo ("Total: " . $total . "\\n");'}, ['802']), ({13: None}, ['713']),
    ({11: None}, ['706']), ({6: 'while $i < 3 {'}, ['601', '602']), ({20: '        if () {'}, ['703']),
    ({19: [a(19)[:-2], '        {']}, ['610']), ({12: ['    }', '  else {']}, ['718']), ({30: '\t\t' + a(30).strip()}, ['608']),
    ({3: '$1notas = array(70, 85, 90);'}, ['201']), ({4: 'total = 0;'}, ['206']), ({2: '/ Programa'}, ['401']),
    ({18: '    $j = 0 / 0;'}, ['902']), ({9: '        $total = $Total + $notas[$i];'}, ['901']),
    ({23: '        $j = $j + 1'}, []), ({15: '    $i = $i + 1;   // siguiente'}, []),
]
def tocadas(c): return {k for n in c for k in (n - 1, n, n + 1)}
rnd = random.Random(2026)
for tam, total in ((2, 300), (3, 300)):
    hechos = 0
    while hechos < total:
        sel = rnd.sample(ESPEC, tam)
        lineas = [tocadas(x[0]) for x in sel]
        if any(lineas[i] & lineas[j] for i in range(tam) for j in range(i + 1, tam)):
            continue
        cambios = {}
        esperado = []
        for c_, e_ in sel:
            cambios.update(c_)
            esperado += e_
        if (18 in cambios and 23 in cambios) or (4 in cambios and 9 in cambios):
            continue  # 4+9: el 901 de $Total se omite a proposito (se parece a la asignacion con error)
        caso(f'H{tam}_{hechos:03d}', mutar(A, cambios), esperado)
        hechos += 1

# ======================= ejecutar ==========================================================
shutil.rmtree(d, ignore_errors=True)
os.makedirs(d)
shutil.copy(jar, d)
seleccion = [x for x in casos if filtro in x[0]]
for nombre, contenido, _ in seleccion:
    open(f'{d}/{nombre}.php', 'wb').write(contenido)
r = subprocess.run(['java', '-jar', os.path.basename(jar)], cwd=d, capture_output=True, text=True, errors='replace')
mal = 0
for nombre, _, esperados in seleccion:
    ruta = f'{d}/{nombre}-errores.log'
    if not os.path.exists(ruta):
        mal += 1
        print(f'SIN LOG  {nombre}')
        continue
    log = open(ruta, 'rb').read().decode('utf-8', 'replace')
    obtenidos = re.findall(r'(?:^|[\r\n])Error (\d{3})\.', log)
    if esperados is None:
        continue  # solo debe terminar y generar el .log
    if collections.Counter(obtenidos) != collections.Counter(esperados):
        mal += 1
        print(f'DIFERENTE {nombre}: esperado {sorted(esperados)} obtenido {sorted(obtenidos)}')
print(f'Bateria extrema: {len(seleccion)} casos  correctos: {len(seleccion) - mal}  diferentes: {mal}  inesperado en consola: {r.stdout.count("inesperado") + r.stdout.count("Exception")}')
