#!/usr/bin/env python3
"""Compila os stubs; as classes com erro (internas do SDK que o codigo da equipe nao usa) viram placeholders vazios."""
import re, subprocess, sys, os, glob
src, out = sys.argv[1], sys.argv[2]
replaced = []
for it in range(12):
    files = glob.glob(src + '/**/*.java', recursive=True)
    subprocess.run(['rm', '-rf', out]); os.makedirs(out)
    r = subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-proc:none', '-source', '11', '-target', '11',
                        '-Xmaxerrs', '5000', '-d', out] + files, capture_output=True, text=True)
    errs = [l for l in r.stderr.splitlines() if re.search(r'\.java:\d+: error:', l)]
    print('iteracao', it, 'erros', len(errs), file=sys.stderr)
    if not errs: break
    made = 0
    for l in r.stderr.splitlines():
        mm = re.search(r'package ([\w.]+) does not exist', l)
        if mm:
            qn = mm.group(1).split('.')
            k = next((i for i, x in enumerate(qn) if x[:1].isupper()), None)
            if k is None:                       # pacote inexistente: o proximo nome capitalizado vem na linha do codigo
                continue
            path = os.path.join(src, *qn[:k], qn[k] + '.java')
            if not os.path.exists(path):
                os.makedirs(os.path.dirname(path), exist_ok=True)
                open(path, 'w').write('package %s;\n\npublic class %s {\n    public %s() {}\n}\n' % ('.'.join(qn[:k]), qn[k], qn[k]))
                made += 1
    if made: continue
    prot = set(open('/tmp/initial_classes.txt').read().split(','))
    prot = {x[:-1].replace('.', '/') + '.java' for x in prot}
    bad = {l.split(':')[0] for l in errs}
    crit = [f for f in bad if any(f.endswith(x) for x in prot)]
    if crit: print('ERRO EM CLASSE ESSENCIAL:', crit, file=sys.stderr)
    bad = {f for f in bad if f not in crit}
    if not bad: break
    for f in bad:
        text = open(f).read()
        pkg = re.search(r'^package ([\w.]+);', text, re.M)
        m = re.search(r'^(?:public |abstract |final )*(class|interface|enum|@interface) (\w+)(<[^{]*?>)?', text, re.M)
        name = os.path.basename(f)[:-5]
        arity = 0
        if m and m.group(3):
            depth, arity = 0, 1
            for ch in m.group(3):
                if ch == '<': depth += 1
                elif ch == '>': depth -= 1
                elif ch == ',' and depth == 1: arity += 1
        kind = 'class'
        exc = ' extends Exception' if re.search(r'extends [\w.]*(Exception|Error|Throwable)\b', text.split('{')[0]) else ''
        tp = ('<' + ', '.join('T%d' % i for i in range(arity)) + '>') if arity else ''
        body = 'public %s %s%s%s {\n    public %s() {}\n}\n' % (kind, name, tp, exc, name)
        open(f, 'w').write(('package %s;\n\n' % pkg.group(1) if pkg else '') + body)
        replaced.append(f)
print('placeholders por erro:', len(replaced), file=sys.stderr)
open('/tmp/replaced.txt', 'w').write('\n'.join(sorted(set(replaced))))
if errs: print('AINDA COM ERROS', len(errs)); sys.exit(1)
