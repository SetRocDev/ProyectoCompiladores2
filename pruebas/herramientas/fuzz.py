"""Genera mutaciones aleatorias del codigo base y verifica que el programa
nunca falle: cada .php debe producir su .log y en la consola no debe aparecer
ningun aviso de que el analisis se detuvo (una excepcion interna).

Uso:  python3 fuzz.py SEMILLA CANTIDAD JAR PHP_BASE CARPETA

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import random, subprocess, sys, os, shutil
semilla=int(sys.argv[1]); n=int(sys.argv[2]); jar=sys.argv[3]; base=open(sys.argv[4],'rb').read()
d=sys.argv[5]; shutil.rmtree(d,ignore_errors=True); os.makedirs(d); shutil.copy(jar,d)
r=random.Random(semilla); simbolos=[b'{',b'}',b'(',b')',b';',b'"',b"'",b'/',b'#',b'$',b'\t',b'?>',b'<?php',b'else',b'while',b'if',b'echo',b'\r\n',b'\n',b'=']
for k in range(n):
    b=bytearray(base)
    for _ in range(r.randint(1,4)):
        op=r.random(); lineas=bytes(b).split(b'\r\n')
        if op<0.3 and len(b)>1: i=r.randrange(len(b)); del b[i:i+r.randint(1,3)]
        elif op<0.6: i=r.randrange(len(b)+1); b[i:i]=r.choice(simbolos)
        elif op<0.8 and len(lineas)>1: j=r.randrange(len(lineas)); del lineas[j]; b=bytearray(b'\r\n'.join(lineas))
        else: j=r.randrange(len(lineas)); lineas.insert(j,lineas[j]); b=bytearray(b'\r\n'.join(lineas))
    open(f'{d}/m{k:04d}.php','wb').write(bytes(b))
out=subprocess.run(['java','-jar',os.path.basename(jar)],cwd=d,capture_output=True,text=True,errors='replace').stdout
faltan=[f for f in os.listdir(d) if f.endswith('.php') and not os.path.exists(f'{d}/'+f[:-4]+'-errores.log')]
fallos=[l for l in out.splitlines() if 'se detuvo' in l or 'Exception' in l or 'inesperado' in l]
print(f"mutaciones: {n}  sin .log: {len(faltan)}  fallos internos: {len(fallos)}")
for l in fallos[:10]: print('  ',l[:200])
