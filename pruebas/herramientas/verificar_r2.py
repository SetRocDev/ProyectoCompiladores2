"""Verifica la regla R2 sobre un .log: al quitar la numeracion y los renglones
de error, debe quedar el .php original byte por byte; los errores deben usar el
salto de linea de la linea que tienen encima.

Herramienta de verificacion (no es parte del programa). Uso de IA: generada con
apoyo de Claude Code y revisada por el estudiante."""
import re, sys
def renglones(b):
    out=[]; i=0; ini=0
    while i < len(b):
        c=b[i:i+1]
        if c in (b'\r', b'\n'):
            t=b'\r\n' if b[i:i+2]==b'\r\n' else c
            out.append((b[ini:i], t)); i+=len(t); ini=i
        else: i+=1
    if ini < len(b) or not out: out.append((b[ini:], b''))
    return out
def verificar(php, log):
    orig=open(php,'rb').read(); logb=open(log,'rb').read()
    BOM=b'\xef\xbb\xbf'
    if orig.startswith(BOM) != logb.startswith(BOM): return ["el BOM del .php no coincide con el del .log"]
    if orig.startswith(BOM): orig=orig[3:]; logb=logb[3:]
    rs=renglones(logb)
    rec=b''; esperado=1; problemas=[]; salto_linea=None
    numerados=[k for k,(t,_) in enumerate(rs) if not re.match(rb'^Error \d{3}\. ', t)]
    for k,(t,s) in enumerate(rs):
        if re.match(rb'^Error \d{3}\. ', t):
            if s and salto_linea is not None and s!=salto_linea: problemas.append(f"renglon {k+1}: el error usa {s!r} y su linea {salto_linea!r}")
            continue
        m=re.match(rb'^(\d{4}) ', t)
        if not m or int(m.group(1))!=esperado: problemas.append(f"renglon {k+1}: numeracion incorrecta {t[:6]!r}"); break
        salto_linea=s
        if k==numerados[-1] and rs[-1][1]==b'': s=b''
        rec+=t[5:]+s; esperado+=1
    if rec!=orig: problemas.append("al quitar numeracion y errores NO queda el original")
    return problemas
if __name__=='__main__':
    malos=0
    for php in sys.argv[1:]:
        log=re.sub(r'\.php$','-errores.log',php)
        if '/casos/' in php: log=log.replace('/casos/','/esperados/')
        p=verificar(php,log)
        if p: malos+=1; print("FALLA",php,p[:3])
    print(f"R2 verificado en {len(sys.argv)-1} archivos; fallas: {malos}")
