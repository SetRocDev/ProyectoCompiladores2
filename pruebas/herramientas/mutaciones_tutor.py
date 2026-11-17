"""Bateria sistematica de mutaciones tipo tutor sobre CodigoProyecto.php.
Cada mutacion declara el multiconjunto EXACTO de codigos de error esperados.

Uso:  python3 mutaciones_tutor.py JAR PHP_BASE CARPETA [ERRORES_POR_CASO CANTIDAD SEMILLA]
      (por defecto: 400 combinaciones de 2 errores con la semilla 7)

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import os, re, shutil, subprocess, sys, collections
jar, base_php, d = sys.argv[1], sys.argv[2], sys.argv[3]
por_caso = int(sys.argv[4]) if len(sys.argv) > 4 else 2
cantidad = int(sys.argv[5]) if len(sys.argv) > 5 else 400
semilla = int(sys.argv[6]) if len(sys.argv) > 6 else 7
B = open(base_php,'rb').read().decode('utf-8').split('\r\n')   # 53 lineas
casos = []   # (nombre, lineas, esperados)
simples = []  # (nombre, cambios, esperados) para combinarlas despues
def mut(nombre, cambios, esperados):
    simples.append((nombre, dict(cambios), list(esperados)))
    l = list(B)
    for i, v in sorted(cambios.items(), reverse=True):     # i = numero de linea (1..53)
        if v is None: del l[i-1]
        elif isinstance(v, list): l[i-1:i] = v
        else: l[i-1] = v
    casos.append((nombre, l, collections.Counter(esperados)))
def L(n): return B[n-1]

# 1. Identificadores a la izquierda del '=' (R3): 201 202 203 206 y 204
asign = {5:'$nombre_estudiante', 6:'$cantidad_notas', 9:'$notas', 16:'$suma_notas', 22:'$promedio',
         35:'$contador_revision', 43:'$mensaje_final', 47:'$total', 48:'$Total', 50:'$resultado'}
for n, v in asign.items():
    nombre = v[1:]
    mut(f'id{n}_digito', {n: L(n).replace(v, '$1'+nombre, 1)}, ['201'])
    mut(f'id{n}_simbolo', {n: L(n).replace(v, '$-'+nombre, 1)}, ['202'])
    mut(f'id{n}_arroba', {n: L(n).replace(v, '$'+nombre[:2]+'@'+nombre[2:], 1)}, ['203'])
    mut(f'id{n}_sin_dolar', {n: L(n).replace(v, nombre, 1)}, ['206'])
# reservada en una variable nueva (no deja otra sin asignar)
for p in ['if', 'else', 'while', 'echo', 'class', 'function', 'return', 'true', 'trim', 'IF', 'Echo']:
    mut(f'reservada_{p}', {46: [L(46), f'${p} = 1;']}, ['204'])
# variable y '=' en lineas distintas
mut('reservada_multilinea', {46: [L(46), '$for', '    = 5;']}, ['204'])
mut('sin_dolar_multilinea', {46: [L(46), 'contador', '    = 5;']}, ['206'])

# 2. Etiquetas (R4)
mut('apertura_espacio', {1: ' <?php'}, ['301'])
mut('apertura_texto_previo', {1: ['echo "";', '<?php']}, ['301'])
mut('apertura_linea_vacia', {1: ['', '<?php']}, ['301'])
mut('apertura_mal_1', {1: '<? php'}, ['302'])
mut('apertura_mal_2', {1: '<?PHP'}, ['302'])
mut('apertura_mal_3', {1: '<php'}, ['302'])
mut('apertura_falta', {1: None}, ['303'])
mut('cierre_mal', {53: '? >'}, ['304'])
mut('cierre_falta', {53: None}, ['305'])
mut('cierre_misma_linea', {53: '?> echo "x";'}, ['306'])
mut('cierre_linea_siguiente', {53: ['?>', 'echo "x";']}, ['307'])
mut('cierre_espacios', {53: '?>  '}, ['308'])
mut('cierre_salto', {53: ['?>', '']}, ['308'])

# 3. Comentarios (R5)
for n in [2, 8, 11, 15, 24, 27, 34, 42, 46]:
    mut(f'coment{n}_una_barra', {n: L(n).replace('//', '/', 1)}, ['401'])
    mut(f'coment{n}_sin_marca', {n: L(n).replace('// ', '', 1)}, ['402'])
mut('coment4_sin_marca', {4: L(4).replace('# ', '', 1)}, ['402'])
mut('coment_continua', {8: [L(8), 'continua aqui']}, ['402'])

# 4. While (R3 del P2): lineas 37-40
W = L(37)
mut('while_sin_parentesis', {37: W.replace('(', '', 1).replace(')', '', 1)}, ['601', '602'])
mut('while_sin_apertura', {37: W.replace('(', '', 1)}, ['601'])
mut('while_sin_cierre_par', {37: W.replace(')', '', 1)}, ['602'])
mut('while_vacia', {37: 'while () {'}, ['603'])
mut('while_sin_llave', {37: W[:-2]}, ['604'])
mut('ok_while_llave_otra_linea', {37: [W[:-2], '{']}, [])  # la profesora: no es error
mut('while_llave_sola_desalineada', {37: [W[:-2], '  {']}, ['610'])
mut('while_sin_cierre', {40: None}, ['605'])
mut('while_vacio', {38: None, 39: None}, ['606'])
mut('while_comentario', {38: '    // nada', 39: None}, ['606'])
mut('while_mal_escrito', {37: W.replace('while', 'whlie')}, ['504'])
for n in [38, 39]:
    t = L(n).lstrip()
    mut(f'while{n}_2esp', {n: '  '+t}, ['607'])
    mut(f'while{n}_6esp', {n: '      '+t}, ['607'])
    mut(f'while{n}_0esp', {n: t}, ['607'])
    mut(f'while{n}_tab', {n: '\t'+t}, ['608'])
mut('while_cierre_2esp', {40: '  }'}, ['609'])

# 5. If / else (R4 del P2): lineas 28-32
I = L(28)
mut('if_sin_parentesis', {28: I.replace('(', '', 1).replace(')', '', 1)}, ['701', '702'])
mut('if_sin_cierre_par', {28: I.replace(')', '', 1)}, ['702'])
mut('if_vacia', {28: 'if () {'}, ['703'])
mut('if_sin_llave', {28: I[:-2]}, ['704'])
mut('ok_if_llave_otra_linea', {28: [I[:-2], '{']}, [])
mut('if_llave_sola_desalineada', {28: [I[:-2], '    {']}, ['710'])
mut('if_sin_cierre', {30: 'else {'}, ['705'])
mut('if_vacio', {29: None}, ['706'])
mut('if_2esp', {29: '  '+L(29).lstrip()}, ['707'])
mut('if_tab', {29: '\t'+L(29).lstrip()}, ['708'])
mut('if_cierre_2esp', {30: '  } else {'}, ['709'])
mut('else_sin_llave', {30: '} else'}, ['711'])
mut('else_vacio', {31: None}, ['713'])
mut('ok_else_otra_linea', {30: ['}', 'else {']}, [])
mut('else_desalineado', {30: ['}', '  else {']}, ['718'])
mut('ok_else_llave_otra_linea', {30: ['} else', '{']}, [])
mut('else_llave_sola_desalineada', {30: ['} else', '    {']}, ['719'])
mut('ok_if_else_estilo_allman', {28: [I[:-2], '{'], 30: ['}', 'else', '{']}, [])
mut('else_6esp', {31: '      '+L(31).lstrip()}, ['715'])
mut('else_tab', {31: '\t'+L(31).lstrip()}, ['716'])
mut('else_cierre_2esp', {32: '  }'}, ['717'])
mut('else_mal_escrito', {30: '} els {'}, ['504'])
mut('else_sin_if', {46: [L(46), 'else {', '    echo "x";', '}']}, ['714'])

# 6. Echo (R5 del P2): las 8 lineas con echo
for n in [12, 13, 25, 29, 31, 38, 44, 51]:
    t = L(n); ind = t[:len(t)-len(t.lstrip())]; expr = t.strip()[5:-1]
    mut(f'echo{n}_sin_pyc', {n: t[:-1]}, ['805'])
    mut(f'echo{n}_parentesis', {n: f'{ind}echo ({expr});'}, ['802'])
    mut(f'echo{n}_vacio', {n: f'{ind}echo;'}, ['801'])
    if '"' in expr:
        k = expr.index('"', 1)  # comilla de cierre de la primera cadena
        # la primera cadena sin su comilla de cierre: si en la linea hay otra comilla, la cadena
        # se empareja con ella y el resto queda fuera (caso L38): 501 por la '\' suelta, 806 y 803
        resto = expr[k+1:]
        esperado = ['803'] if '"' not in resto else ['501', '806', '803']
        mut(f'echo{n}_comilla', {n: f'{ind}echo {expr[:k]}{resto};'}, esperado)
        # la ultima cadena sin su comilla de cierre (el caso mas comun)
        j = expr.rindex('"')
        mut(f'echo{n}_comilla_final', {n: f'{ind}echo {expr[:j]}{expr[j+1:]};'}, ['803'])
mut('echo_mal_escrito', {44: 'eco $mensaje_final;'}, ['504'])
mut('echo_mal_escrito_2', {12: 'ehco "Estudiante: $nombre_estudiante\\n";'}, ['504'])
mut('ok_echo_cadena_larga', {44: 'echo "' + 'x' * 5000 + '";'}, [])
mut('ok_echo_numeros_php', {44: 'echo 1e3 + 0x1F + 0b101 + 1_000;'}, [])
mut('echo_simple_abierta', {44: "echo 'Proceso;"}, ['804'])
mut('echo_operador_colgando', {44: 'echo $mensaje_final . ;'}, ['806'])
mut('echo_falta_operador', {44: 'echo "Final: " $mensaje_final;'}, ['806'])

# 7. for (611-620): se valida con las mismas reglas que el while
FOR = L(18)
# 7. for: la profesora aclaró que no se valida (bloque genérico: solo que cierre su llave)
mut('for_sin_llave_cierre', {20: None}, ['505'])
mut('for_sin_llave_apertura', {18: FOR.replace(' {', '')}, ['503'])
mut('ok_for_cuerpo_2esp', {19: '  '+L(19).lstrip()}, [])
mut('ok_for_cuerpo_tab', {19: '\t'+L(19).lstrip()}, [])
mut('ok_for_cierre_1esp', {20: ' }'}, [])
mut('ok_for_llave_abajo', {18: [FOR.replace(' {', ''), '{']}, [])
mut('ok_for_vacio', {19: '    // pendiente'}, [])
mut('ok_for_sin_espacios', {18: FOR.replace('for (', 'for(').replace(') {', '){')}, [])
mut('ok_for_comas', {18: FOR.replace('$i = 0;', '$i = 0, $j = 0;')}, [])
mut('ok_for_mayuscula', {18: FOR.replace('for', 'For', 1)}, [])

# 7b. Semantico 902 (asignacion de un valor fijo en la condicion) y 903 (division entre 0)
mut('ok_cond_asigna_if', {28: L(28).replace('>=', '=')}, [])  # la condicion se considera correcta
mut('ok_cond_asigna_while', {37: 'while ($contador_revision = 4) {'}, [])
mut('div902_asig', {22: L(22).replace('$cantidad_notas;', '0;')}, ['902'])
mut('div902_echo', {44: 'echo $mensaje_final . 10 % 0;'}, ['902'])
mut('div902_echo_agrupado', {44: 'echo $mensaje_final . (10 % 0);'}, ['802', '902'])  # Q10: parentesis en echo es 802
mut('ok_cond_doble_igual', {28: L(28).replace('>=', '==')}, [])
mut('ok_division_decimal', {22: L(22).replace('$cantidad_notas;', '0.5;')}, [])

# 7c. Variantes de espacios validas en PHP (no deben dar error)
mut('ok_while_sin_espacios', {37: L(37).replace('while (', 'while(').replace(') {', '){')}, [])
mut('ok_else_pegado', {30: '}else{'}, [])
mut('ok_if_sin_espacios', {28: L(28).replace('if (', 'if(').replace(') {', '){')}, [])
mut('ok_comentario_tras_llave', {28: L(28) + ' // evaluar', 37: L(37) + ' # revisar'}, [])
mut('ok_echo_sin_espacio', {12: 'echo"Estudiante: $nombre_estudiante\\n";'}, [])

# 7d. Expresiones validas de PHP dentro de un echo (T30): no deben dar error
for k, e in enumerate(['$total ** 2', '$total <=> $Total', '$total and $Total', '$resultado++', '-$promedio',
                       '$promedio ?: "x"', '$total << 1', '$total instanceof stdClass', '$notas[0] % 2', '"Fin (ok)"']):
    mut(f'ok_echo_expr{k}', {52: [L(52), f'echo {e};']}, [])
mut('echo_parentesis_sin_cerrar', {44: 'echo ("Final: " . $mensaje_final;'}, ['802', '806'])
mut('ok_cond_asigna_arreglo', {28: 'if ($notas[0] = 70) {'}, [])
# la profesora: tampoco se permiten los parentesis de una funcion o de una conversion en el echo
for k, e in enumerate(['count($notas)', '(int) $promedio', '$notas[count($notas) - 1]', 'strtoupper($mensaje_final) . "\\n"']):
    mut(f'echo_parentesis_funcion{k}', {44: f'echo {e};'}, ['802'])
mut('div902_modulo', {50: [L(50), '$resultado %= 0;']}, ['902'])

# 8. Semantico: cambiar la mayuscula de una variable que se USA
usos = {12: '$nombre_estudiante', 22: '$cantidad_notas', 25: '$promedio', 28: '$promedio', 44: '$mensaje_final', 51: '$resultado', 38: '$notas'}
for n, v in usos.items():
    otra = v[0]+(v[1].upper() if v[1].islower() else v[1].lower())+v[2:]
    k = L(n).rindex(v)
    mut(f'sem{n}_mayuscula', {n: L(n)[:k]+otra+L(n)[k+len(v):]}, ['901'])

# 9. Transformaciones VALIDAS: no deben dar ningun error
mut('ok_lineas_en_blanco', {38: ['', L(38), ''], 29: [L(29), '']}, [])
mut('ok_comentarios_en_bloques', {38: ['    // revisar', L(38) + ' // fin'], 31: ['    # nota', L(31)]}, [])
mut('ok_echo_en_dos_lineas', {44: ['echo "Mensaje: "', '    . $mensaje_final;']}, [])
mut('ok_if_anidado', {39: [L(39), '    if ($contador_revision == 2) {', '        echo "Mitad\\n";', '    }']}, [])
mut('ok_else_if', {30: ['} else if ($promedio >= 60) {', '    echo "Reposicion\\n";', '} else {']}, [])
mut('ok_cadenas_raras', {44: 'echo "{ ( ; # // ?> " . $mensaje_final;'}, [])
mut('ok_division_multilinea', {22: ['$promedio = $suma_notas', '    / $cantidad_notas;']}, [])
mut('ok_mayusculas_palabras', {28: I.replace('if', 'IF'), 37: W.replace('while', 'While'), 44: 'ECHO $mensaje_final;'}, [])
mut('ok_while_varias_sentencias', {39: [L(39), '    $suma_notas = $suma_notas + 0;']}, [])

# 10. Combinaciones: dos mutaciones en lineas distintas -> se esperan los errores de ambas
mut('combo_parentesis_array_y_while', {9: '$notas = array(85, 90, 78, 92;', 37: 'while () {'}, ['603'])
import random
r = random.Random(semilla)
candidatas = [m for m in simples if not m[0].startswith(('sem', 'ok_', 'reservada', 'combo')) and '901' not in m[2]]
def lineas_tocadas(c):  # una insercion en la linea n tambien "toca" sus vecinas
    return {k for n in c for k in (n - 1, n, n + 1)}
hechas = 0
while hechas < cantidad:
    grupo = r.sample(candidatas, por_caso)
    prefijos = {m[0].split('_')[0][:4] for m in grupo}
    zonas = [lineas_tocadas(m[1]) for m in grupo]
    if len(prefijos) < por_caso or any(zonas[i] & zonas[j] for i in range(por_caso) for j in range(i + 1, por_caso)):
        continue
    if any(n in m[1] for m in grupo for n in (1, 53)):
        continue  # las etiquetas se combinan aparte
    cambios = {}
    esperado = collections.Counter()
    for m in grupo:
        cambios.update(m[1])
        esperado += collections.Counter(m[2])
    l = list(B)
    for i, v in sorted(cambios.items(), reverse=True):
        if v is None: del l[i-1]
        elif isinstance(v, list): l[i-1:i] = v
        else: l[i-1] = v
    casos.append((f'combo{por_caso}_{hechas:03d}__' + '__'.join(m[0] for m in grupo), l, esperado))
    hechas += 1

shutil.rmtree(d, ignore_errors=True); os.makedirs(d); shutil.copy(jar, d)
for nombre, lineas, _ in casos:
    open(f'{d}/{nombre}.php','wb').write('\r\n'.join(lineas).encode('utf-8'))
subprocess.run(['java','-jar',os.path.basename(jar)], cwd=d, capture_output=True)
mal = 0
for nombre, _, esperados in casos:
    log = open(f'{d}/{nombre}-errores.log', encoding='utf-8').read()
    obtenidos = collections.Counter(re.findall(r'(?:^|[\r\n])Error (\d{3})\.', log))
    if obtenidos != esperados:
        mal += 1
        print(f'DIFERENTE {nombre}: esperado {sorted(esperados.elements())} obtenido {sorted(obtenidos.elements())}')
print(f'Mutaciones tipo tutor: {len(casos)}  correctas: {len(casos)-mal}  diferentes: {mal}')
