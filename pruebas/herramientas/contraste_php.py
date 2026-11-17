"""Contraste con PHP real (necesita el comando php):
  1. Los casos de pruebas/casos que no tienen ningun error deben ser PHP valido (php -l).
  2. Al ejecutar con PHP los casos validos, las variables de los avisos
     "Undefined variable" deben ser exactamente las del Error 901.

Uso:  python3 contraste_php.py            (desde cualquier carpeta)

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import glob, os, re, subprocess

PRUEBAS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
validos = rechazados = coinciden = difieren = 0
for php in sorted(glob.glob(os.path.join(PRUEBAS, 'casos', 'P*.php'))):
    nombre = os.path.basename(php)[:-4]
    log = open(os.path.join(PRUEBAS, 'esperados', nombre + '-errores.log'), 'rb').read().decode('utf-8', 'replace')
    errores = re.findall(r'(?:^|[\r\n])Error (\d{3})\.', log)
    es_valido = subprocess.run(['php', '-l', php], capture_output=True).returncode == 0
    if not errores:
        if es_valido:
            validos += 1
        else:
            rechazados += 1
            print('SIN ERRORES PERO PHP LO RECHAZA:', nombre)
    if es_valido:
        nuestras = set(re.findall(r"Error 901\. \[Semántico\] La variable '(\$\w+)'", log))
        r = subprocess.run(['timeout', '5', 'php', '-d', 'display_errors=stderr', '-d', 'error_reporting=E_ALL', php],
                           capture_output=True, text=True, errors='replace')
        de_php = set('$' + v for v in re.findall(r'Undefined variable \$(\w+)', r.stdout + r.stderr))
        if nuestras == de_php:
            coinciden += 1
        else:
            difieren += 1
            print(f'901 DISTINTO {nombre}: nuestro {sorted(nuestras)}  PHP {sorted(de_php)}')
print(f'Casos sin errores aceptados por PHP: {validos} (rechazados: {rechazados})')
print(f'Error 901 = avisos "Undefined variable" de PHP: {coinciden} de {coinciden + difieren}')
