#!/usr/bin/env python3
"""Le arquivos .dex (formato Android) e extrai a API publica de classes (hierarquia, campos, metodos, generics,
constantes) para gerar stubs Java (.java so com assinaturas). Serve para compilar codigo FTC no desktop sem as fontes
do SDK. Uso: dexapi.py <saida_dir> <prefixo1,prefixo2,...> <arquivo.dex>..."""
import struct, sys, os, re, json

PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL = 1, 2, 4, 8, 0x10
VOLATILE_BRIDGE, VARARGS_TRANSIENT, NATIVE, INTERFACE, ABSTRACT = 0x40, 0x80, 0x100, 0x200, 0x400
SYNTHETIC, ANNOTATION, ENUM = 0x1000, 0x2000, 0x4000
PRIM = {'V': 'void', 'Z': 'boolean', 'B': 'byte', 'S': 'short', 'C': 'char', 'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double'}


def uleb(d, p):
    r = s = 0
    while True:
        b = d[p]; p += 1
        r |= (b & 0x7f) << s; s += 7
        if not b & 0x80: return r, p


class Dex:
    def __init__(self, path, prefixes):
        d = self.d = open(path, 'rb').read()
        (ss, so, ts, to, ps, po, fs, fo, ms, mo, cs, co) = struct.unpack_from('<12I', d, 0x38)
        self.strs = []
        for i in range(ss):
            off = struct.unpack_from('<I', d, so + 4 * i)[0]
            _, p = uleb(d, off)
            self.strs.append(d[p:d.index(b'\0', p)].decode('utf-8', 'replace'))
        self.types = [self.strs[struct.unpack_from('<I', d, to + 4 * i)[0]] for i in range(ts)]
        self.protos = []
        for i in range(ps):
            sh, rt, poff = struct.unpack_from('<III', d, po + 12 * i)
            params = []
            if poff:
                n = struct.unpack_from('<I', d, poff)[0]
                params = [self.types[x] for x in struct.unpack_from('<%dH' % n, d, poff + 4)]
            self.protos.append((self.types[rt], params))
        self.fields = [struct.unpack_from('<HHI', d, fo + 8 * i) for i in range(fs)]
        self.methods = [struct.unpack_from('<HHI', d, mo + 8 * i) for i in range(ms)]
        self.defs = {}
        for i in range(cs):
            (ci, acc, sup, ifs, src, ann, cdata, sval) = struct.unpack_from('<8I', d, co + 32 * i)
            self.defs[self.types[ci]] = (acc, sup, ifs, ann, cdata, sval)

    def load(self, name):
        acc, sup, ifs, ann, cdata, sval = self.defs[name]
        return self.read_class(name, acc, sup, ifs, ann, cdata, sval)

    # ---------- valores e anotacoes ----------
    def value(self, p):
        d = self.d
        b = d[p]; p += 1
        t, a = b & 0x1f, b >> 5
        def sized(n, signed=False, fill_right=False):
            raw = d[p:p + n]
            if fill_right: raw = raw + bytes(8 - n) if n < 8 else raw
            return int.from_bytes(raw, 'little', signed=signed)
        if t in (0x00, 0x02, 0x04, 0x06, 0x03):
            n = a + 1
            v = int.from_bytes(d[p:p + n], 'little', signed=(t != 0x03)); return ('int', v), p + n
        if t == 0x10:
            n = a + 1; raw = d[p:p + n] + bytes(4 - n) if False else bytes(4 - n) + d[p:p + n]
            return ('float', struct.unpack('<f', bytes(4 - n) + d[p:p + n])[0]), p + n
        if t == 0x11:
            n = a + 1
            return ('double', struct.unpack('<d', bytes(8 - n) + d[p:p + n])[0]), p + n
        if t == 0x17: return ('str', self.strs[int.from_bytes(d[p:p + a + 1], 'little')]), p + a + 1
        if t == 0x18: return ('type', self.types[int.from_bytes(d[p:p + a + 1], 'little')]), p + a + 1
        if t == 0x1b:
            f = self.fields[int.from_bytes(d[p:p + a + 1], 'little')]
            return ('enum', self.types[f[0]], self.strs[f[2]]), p + a + 1
        if t in (0x19, 0x1a, 0x15, 0x16): return ('ref', None), p + a + 1
        if t == 0x1c:
            n, p = uleb(d, p); out = []
            for _ in range(n):
                v, p = self.value(p); out.append(v)
            return ('arr', out), p
        if t == 0x1d:
            an, p = self.annotation(p); return ('ann', an), p
        if t == 0x1e: return ('null', None), p
        if t == 0x1f: return ('bool', bool(a)), p
        return ('?', None), p

    def annotation(self, p):
        ti, p = uleb(self.d, p); n, p = uleb(self.d, p)
        el = {}
        for _ in range(n):
            ni, p = uleb(self.d, p)
            v, p = self.value(p); el[self.strs[ni]] = v
        return (self.types[ti], el), p

    def ann_set(self, off):
        out = {}
        if not off: return out
        n = struct.unpack_from('<I', self.d, off)[0]
        for i in range(n):
            ao = struct.unpack_from('<I', self.d, off + 4 + 4 * i)[0]
            (t, el), _ = self.annotation(ao + 1)
            out[t] = el
        return out

    @staticmethod
    def plain(v):
        k = v[0]
        if k == 'arr': return [Dex.plain(x) for x in v[1]]
        if k in ('str', 'type'): return v[1]
        if k == 'int' or k == 'bool': return v[1]
        return v

    # ---------- classe ----------
    def read_class(self, name, acc, sup, ifs, ann, cdata, sval):
        d = self.d
        c = {'name': name, 'acc': acc, 'super': self.types[sup] if sup != 0xffffffff else None,
             'ifaces': [], 'fields': [], 'methods': [], 'sig': None, 'inner': None, 'adefault': {}}
        if ifs:
            n = struct.unpack_from('<I', d, ifs)[0]
            c['ifaces'] = [self.types[x] for x in struct.unpack_from('<%dH' % n, d, ifs + 4)]
        fann, mann = {}, {}
        if ann:
            cao, fsz, msz, psz = struct.unpack_from('<4I', d, ann)
            cs = self.ann_set(cao)
            if 'Ldalvik/annotation/Signature;' in cs:
                c['sig'] = ''.join(self.plain(cs['Ldalvik/annotation/Signature;']['value']))
            if 'Ldalvik/annotation/InnerClass;' in cs:
                e = cs['Ldalvik/annotation/InnerClass;']
                c['inner'] = (e['accessFlags'][1] if 'accessFlags' in e else 0, e['name'][1] if 'name' in e and e['name'][0] == 'str' else None)
            if 'Ldalvik/annotation/AnnotationDefault;' in cs:
                inner = cs['Ldalvik/annotation/AnnotationDefault;']['value'][1]
                c['adefault'] = inner[1]
            q = ann + 16
            for i in range(fsz):
                fi, ao = struct.unpack_from('<II', d, q + 8 * i); fann[fi] = self.ann_set(ao)
            q += 8 * fsz
            for i in range(msz):
                mi, ao = struct.unpack_from('<II', d, q + 8 * i); mann[mi] = self.ann_set(ao)
        statics = []
        if cdata:
            p = cdata
            sf, p = uleb(d, p); inf, p = uleb(d, p); dm, p = uleb(d, p); vm, p = uleb(d, p)
            idx = 0
            for kind, n in (('s', sf), ('i', inf)):
                idx = 0
                for _ in range(n):
                    diff, p = uleb(d, p); a, p = uleb(d, p); idx += diff
                    ci, ti, ni = self.fields[idx]
                    f = {'name': self.strs[ni], 'type': self.types[ti], 'acc': a, 'static': kind == 's', 'sig': None, 'val': None}
                    sa = fann.get(idx, {}).get('Ldalvik/annotation/Signature;')
                    if sa: f['sig'] = ''.join(self.plain(sa['value']))
                    c['fields'].append(f)
                    if kind == 's': statics.append(f)
            for kind, n in (('d', dm), ('v', vm)):
                idx = 0
                for _ in range(n):
                    diff, p = uleb(d, p); a, p = uleb(d, p); code, p = uleb(d, p); idx += diff
                    ci, pi, ni = self.methods[idx]
                    ret, params = self.protos[pi]
                    m = {'name': self.strs[ni], 'acc': a, 'ret': ret, 'params': params, 'sig': None, 'throws': [], 'code': code != 0}
                    an = mann.get(idx, {})
                    if 'Ldalvik/annotation/Signature;' in an: m['sig'] = ''.join(self.plain(an['Ldalvik/annotation/Signature;']['value']))
                    if 'Ldalvik/annotation/Throws;' in an: m['throws'] = self.plain(an['Ldalvik/annotation/Throws;']['value'])
                    c['methods'].append(m)
        if sval:
            n, p = uleb(d, sval)
            for i in range(n):
                v, p = self.value(p)
                if i < len(statics): statics[i]['val'] = v
        return c


# ---------- assinaturas genericas ----------
class Sig:
    def __init__(self, s): self.s, self.i = s, 0
    def pk(self): return self.s[self.i] if self.i < len(self.s) else ''
    def formals(self):
        res = []
        if self.pk() != '<': return res
        self.i += 1
        while self.pk() != '>':
            j = self.s.index(':', self.i); name = self.s[self.i:j]; self.i = j
            bounds = []
            while self.pk() == ':':
                self.i += 1
                if self.pk() == ':': continue
                bounds.append(self.ftype())
            bounds = [b for b in bounds if b != 'java.lang.Object']
            res.append(name + (' extends ' + ' & '.join(bounds) if bounds else ''))
        self.i += 1
        return res
    def ftype(self):
        c = self.pk()
        if c == 'L': return self.ctype()
        if c == 'T':
            j = self.s.index(';', self.i); n = self.s[self.i + 1:j]; self.i = j + 1; return n
        if c == '[': self.i += 1; return self.ftype() + '[]'
        self.i += 1; return PRIM[c]
    def ctype(self):
        self.i += 1; out = ''
        while True:
            j = self.i
            while self.s[j] not in '<;.': j += 1
            out += self.s[self.i:j].replace('/', '.').replace('$', '.'); self.i = j
            if self.pk() == '<':
                self.i += 1; args = []
                while self.pk() != '>':
                    c = self.pk()
                    if c == '*': self.i += 1; args.append('?')
                    elif c == '+': self.i += 1; args.append('? extends ' + self.ftype())
                    elif c == '-': self.i += 1; args.append('? super ' + self.ftype())
                    else: args.append(self.ftype())
                self.i += 1; out += '<' + ', '.join(args) + '>'
            if self.pk() == '.': self.i += 1; out += '.'; continue
            self.i += 1; return out
    def method(self):
        f = self.formals()
        self.i += 1; ps = []
        while self.pk() != ')': ps.append(self.ftype())
        self.i += 1
        ret = 'void' if self.pk() == 'V' and not self.__setattr__('i', self.i + 1) else self.ftype()
        th = []
        while self.pk() == '^': self.i += 1; th.append(self.ftype())
        return f, ps, ret, th
    def cls(self):
        f = self.formals(); sup = self.ftype(); ifs = []
        while self.i < len(self.s): ifs.append(self.ftype())
        return f, sup, ifs


def jname(desc):
    """Ljava/lang/String; -> java.lang.String ; [I -> int[]"""
    if desc.startswith('['): return jname(desc[1:]) + '[]'
    if desc in PRIM or len(desc) == 1: return PRIM.get(desc, desc)
    return desc[1:-1].replace('/', '.').replace('$', '.')


def erase(t):
    t = re.sub(r'<.*>', '', t)
    return t


def lit(v, typ=None):
    k = v[0]
    if k == 'str': return json.dumps(v[1])
    if k == 'bool': return 'true' if v[1] else 'false'
    if k == 'float':
        x = v[1]
        return 'Float.NaN' if x != x else ('Float.POSITIVE_INFINITY' if x == float('inf') else ('Float.NEGATIVE_INFINITY' if x == float('-inf') else repr(x) + 'f'))
    if k == 'double':
        x = v[1]
        return 'Double.NaN' if x != x else ('Double.POSITIVE_INFINITY' if x == float('inf') else ('Double.NEGATIVE_INFINITY' if x == float('-inf') else repr(x)))
    if k == 'int':
        x = v[1]
        if typ == 'J': return str(x) + 'L'
        if typ == 'Z': return 'true' if x else 'false'
        if typ == 'C': return "(char) %d" % x
        if typ == 'B': return "(byte) %d" % x
        if typ == 'S': return "(short) %d" % x
        if x == -2147483648: return '(-2147483647 - 1)'
        return str(x)
    if k == 'enum': return jname(v[1]) + '.' + v[2]
    if k == 'type': return jname(v[1]) + '.class'
    if k == 'arr': return '{' + ', '.join(lit(x) for x in v[1]) + '}'
    return 'null'


def default_for(t):
    if t in ('int', 'short', 'byte', 'long'): return '0'
    if t in ('float', 'double'): return '0'
    if t == 'boolean': return 'false'
    if t == 'char': return "'\\0'"
    return 'null'


class Gen:
    def __init__(self, classes):
        self.cl = classes                       # desc -> classe
        self.by_outer = {}
        self.missing = set()
        for name, c in classes.items():
            if c['inner'] is not None and '$' in name:
                outer = name[:name.rindex('$')] + ';'
                self.by_outer.setdefault(outer, []).append(name)

    def inners(self, name):
        pre = name[:-1] + '$'
        return [n for n in self.cl if n.startswith(pre) and '$' not in n[len(pre):]]

    def is_anon(self, name):
        c = self.cl[name]
        return c['inner'] is not None and c['inner'][1] is None

    def ref(self, jn):
        """registra tipo referenciado (nome java) para possivel placeholder"""
        self.refs.add(jn)

    def mods(self, acc, iface_member=False, allow=PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL):
        out = []
        if acc & PUBLIC: out.append('public')
        elif acc & PROTECTED: out.append('protected')
        if acc & STATIC: out.append('static')
        if acc & FINAL: out.append('final')
        return ' '.join(out)

    def tparams(self, f): return '<' + ', '.join(f) + '> ' if f else ''

    def super_call(self, c):
        sup = c['super']
        if not sup or sup == 'Ljava/lang/Object;' or sup not in self.cl: return ''
        best = None
        for m in self.cl[sup]['methods']:
            if m['name'] == '<init>' and not (m['acc'] & PRIVATE) and not (m['acc'] & SYNTHETIC):
                ps = self.mparams(m)
                if best is None or len(ps) < len(best): best = ps
        if best is None or not best: return ''
        args = []
        for t in best:
            e = erase(t)
            prim = e in ('int', 'short', 'byte', 'long', 'float', 'double', 'boolean', 'char')
            tvar = ('.' not in e and not prim)          # variavel de tipo (do proprio ou do super)
            args.append(default_for(e) if prim else ('null' if tvar else '(%s) null' % e))
        return 'super(%s);' % ', '.join(args)

    def mparams(self, m):
        if m['sig']:
            try: return Sig(m['sig']).method()[1]
            except Exception: pass
        return [jname(p) for p in m['params']]

    def gen_class(self, name, indent='', shallow=False):
        c = self.cl[name]
        acc = c['acc']
        if c['inner']: acc = c['inner'][0] or acc
        simple = c['inner'][1] if c['inner'] and c['inner'][1] else name[name.rindex('/') + 1:-1]
        self.tvars = set()
        f = []; sup = jname(c['super']) if c['super'] else None; ifs = [jname(i) for i in c['ifaces']]
        if c['sig']:
            try:
                f, sup, ifs = Sig(c['sig']).cls()
            except Exception: pass
        for x in f: self.tvars.add(x.split(' ')[0])
        # supertipos fora do conjunto (e que nao sejam java.*) sao descartados
        def known(jn):
            base = erase(jn)
            return base.startswith('java.') or base.startswith('javax.') or ('L' + base.replace('.', '/') + ';') in self.cl or self.nested_known(base)
        is_iface = bool(acc & INTERFACE); is_enum = bool(acc & ENUM); is_ann = bool(acc & ANNOTATION)
        head = indent + self.mods(acc & ~FINAL if is_enum or is_iface else acc)
        if head.strip(): head += ' '
        if is_ann: head += '@interface ' + simple
        elif is_iface:
            head += 'interface ' + simple + (('<' + ', '.join(f) + '>') if f else '')
            ex = [i for i in ifs if known(i)]
            if ex: head += ' extends ' + ', '.join(ex)
        elif is_enum: head += 'enum ' + simple
        else:
            if acc & ABSTRACT: head += 'abstract '
            head += 'class ' + simple + (('<' + ', '.join(f) + '>') if f else '')
            if sup and sup != 'java.lang.Object' and known(sup): head += ' extends ' + sup
            ex = [i for i in ifs if known(i)]
            if ex: head += ' implements ' + ', '.join(ex)
        if is_enum:
            ex = [i for i in ifs if known(i) and i != 'java.lang.Enum']
            if ex: head += ' implements ' + ', '.join(ex)
        body = []
        ind = indent + '    '
        enum_consts = [x['name'] for x in c['fields'] if x['static'] and x['acc'] & ENUM]
        if is_enum: body.append(ind + ', '.join(enum_consts) + ';')
        for fl in ([] if shallow else c['fields']):
            if fl['acc'] & (PRIVATE | SYNTHETIC) or (fl['acc'] & ENUM) or fl['name'].startswith('$'): continue
            t = jname(fl['type'])
            if fl['sig']:
                try: t = Sig(fl['sig']).ftype()
                except Exception: pass
            m = []
            if fl['acc'] & PUBLIC: m.append('public')
            elif fl['acc'] & PROTECTED: m.append('protected')
            if fl['static'] and not is_iface: m.append('static')
            if fl['acc'] & FINAL and not is_iface: m.append('final')
            init = ''
            if fl['static'] and (fl['acc'] & FINAL):
                init = ' = ' + (lit(fl['val'], fl['type'][0] if len(fl['type']) == 1 else None) if fl['val'] and fl['val'][0] != 'null' and not fl['val'][0] == 'ref' else default_for(erase(t)))
            elif (fl['acc'] & FINAL) or is_iface:
                init = ' = ' + default_for(erase(t))
            if is_iface and not init: init = ' = ' + default_for(erase(t))
            if is_ann: continue
            body.append(ind + (' '.join(m) + ' ' if m else '') + t + ' ' + fl['name'] + init + ';')
        has_ctor = False
        for m in ([] if shallow else c['methods']):
            a = m['acc']
            if a & (PRIVATE | SYNTHETIC) or (a & VOLATILE_BRIDGE and not (a & STATIC)) and not (a & ABSTRACT) and False: continue
            if a & SYNTHETIC or (a & 0x40 and a & 0x1000): continue
            if a & PRIVATE: continue
            n = m['name']
            if n == '<clinit>': continue
            if is_enum and (n in ('values', 'valueOf') or n == '<init>'): continue
            if n.startswith('lambda$') or n.startswith('access$'): continue
            tp, ps, ret, th = [], None, None, []
            if m['sig']:
                try: tp, ps, ret, th = Sig(m['sig']).method()
                except Exception: tp = []; ps = None
            if ps is None:
                ps = [jname(p) for p in m['params']]; ret = jname(m['ret']); th = []
            if not th: th = [jname(t) for t in m['throws']]
            # parametros implicitos de ctor (enum/inner) nao tratados
            mod = []
            if a & PUBLIC: mod.append('public')
            elif a & PROTECTED: mod.append('protected')
            if a & STATIC: mod.append('static')
            if a & FINAL and not is_iface and n != '<init>': mod.append('final')
            if n == '<init>':
                if is_iface or is_ann: continue
                has_ctor = True
            abstract = bool(a & ABSTRACT)
            if abstract and not is_iface: mod.append('abstract')
            if is_iface and not abstract and not (a & STATIC): mod.append('default')
            va = bool(a & VARARGS_TRANSIENT) and ps and ps[-1].endswith('[]')
            plist = []
            for i, t in enumerate(ps):
                if va and i == len(ps) - 1: t = t[:-2] + '...'
                plist.append('%s p%d' % (t, i))
            thr = (' throws ' + ', '.join(th)) if th else ''
            tps = ('<' + ', '.join(tp) + '> ') if tp else ''
            if is_ann:
                dv = c['adefault'].get(n)
                body.append(ind + ret + ' ' + n + '()' + ((' default ' + lit(dv)) if dv else '') + ';')
                continue
            if n == '<init>':
                body.append(ind + ' '.join(mod) + ' ' + tps + simple + '(' + ', '.join(plist) + ')' + thr + ' { ' + self.super_call(c) + ' }')
            elif abstract:
                body.append(ind + ' '.join(mod) + ' ' + tps + ret + ' ' + n + '(' + ', '.join(plist) + ')' + thr + ';')
            else:
                body.append(ind + ' '.join(mod) + ' ' + tps + ret + ' ' + n + '(' + ', '.join(plist) + ')' + thr + ' { throw new UnsupportedOperationException(); }')
        if not shallow and not has_ctor and not is_iface and not is_enum and not is_ann:
            sc = self.super_call(c)
            if sc: body.append(ind + '/* ctor sintetico */ ' + ('public ' if acc & PUBLIC else '') + simple + '() { ' + sc + ' }')
        for inner in ([] if shallow else sorted(self.inners(name))):
            if self.is_anon(inner): continue
            ia = self.cl[inner]['inner'][0] if self.cl[inner]['inner'] else 0
            if not (ia & (PUBLIC | PROTECTED)): continue      # classes aninhadas internas nao fazem parte da API
            body.append(self.gen_class(inner, ind))
            self.tvars = set(x.split(' ')[0] for x in f)
        return head + ' {\n' + '\n'.join(body) + '\n' + indent + '}'

    def nested_known(self, base):
        # nome java com '.' pode ser classe aninhada
        parts = base.split('.')
        for i in range(len(parts) - 1, 0, -1):
            cand = 'L' + '/'.join(parts[:i]) + '$' + '$'.join(parts[i:]) + ';'
            if cand in self.cl: return True
        return False


STR_RE = re.compile(r'"(?:[^"\\]|\\.)*"')
QN_RE = re.compile(r'\b((?:[a-z][a-z0-9_]*\.)+[A-Z][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)')


def resolve(qn, index):
    """nome java qualificado -> descritor existente no indice (tenta aninhadas e remove sufixos como constantes)"""
    parts = qn.split('.')
    first = next((k for k, x in enumerate(parts) if x[:1].isupper()), None)
    if first is None: return None, None
    pkg, rest = parts[:first], parts[first:]
    for n in range(len(rest), 0, -1):
        desc = 'L' + '/'.join(pkg) + ('/' if pkg else '') + '$'.join(rest[:n]) + ';'
        if desc in index: return desc, None
    return None, ('.'.join(pkg), rest[0])


def main():
    out, prefixes, files = sys.argv[1], [p.replace('.', '/') for p in sys.argv[2].split(',')], sys.argv[3:]
    dexes = [Dex(f, prefixes) for f in files]
    index = {}
    for dx in dexes:
        for name in dx.defs: index.setdefault(name, dx)
    inner_idx = {}
    for name in index:
        if '$' in name: inner_idx.setdefault(name[:name.rindex('$')] + ';', []).append(name)
    MAXD = int(os.environ.get('MAXD', '2'))
    cl, level, queue = {}, {}, []
    def add(name, lv):
        if name in level and level[name] <= lv: return
        level[name] = lv
        queue.append(name)
    for name in index:
        if '$' not in name and any(name.startswith('L' + p) for p in prefixes): add(name, 0)
    g = Gen(cl)
    srcs, placeholders, throwing = {}, {}, set()
    shallow_set = set()
    while queue:
        name = queue.pop(0)
        if name in srcs and level[name] >= 0 and name in cl: pass
        # carrega a classe, aninhadas e supertipos (supertipos entram no mesmo nivel)
        def load(n):
            if n in cl: return
            cl[n] = index[n].load(n)
            for inn in inner_idx.get(n, []): load(inn)
            c = cl[n]
            for sup in ([c['super']] if c['super'] else []) + c['ifaces']:
                if sup in index and '$' not in sup:
                    if sup not in level: level[sup] = level[name]; queue.append(sup)
                    load(sup)
        load(name)
    done = set()
    # geracao + expansao por referencias
    pending = [n for n in cl if '$' not in n]
    gen_level = {n: level.get(n, 0) for n in pending}
    while pending:
        name = pending.pop(0)
        if name in done: continue
        done.add(name)
        try:
            src = g.gen_class(name, shallow=(name in shallow_set))
        except Exception as e:
            print('erro em', name, e, file=sys.stderr); continue
        srcs[name] = src
        for m in re.finditer(r'throws ([^{;]+)', src):
            for t in m.group(1).split(','): throwing.add(t.strip())
        text = STR_RE.sub('""', src)
        for m in QN_RE.finditer(text):
            qn = m.group(1)
            desc, miss = resolve(qn, index)
            if desc:
                top = desc.split('$')[0] + (';' if '$' in desc else '')
                if top.endswith(';;'): top = top[:-1]
                top = desc[:desc.index('$')] + ';' if '$' in desc else desc
                if top.startswith(('Lorg/w3c/', 'Lorg/xml/', 'Ljavax/', 'Ljava/')): continue
                if top not in done and top in index and top not in cl:
                    if gen_level.get(name, 0) >= MAXD: shallow_set.add(top)
                    gen_level[top] = gen_level.get(name, 0) + 1
                    level[top] = gen_level[top]
                    def load2(n):
                        if n in cl: return
                        cl[n] = index[n].load(n)
                        for inn in inner_idx.get(n, []): load2(inn)
                        c = cl[n]
                        for sup in ([c['super']] if c['super'] else []) + c['ifaces']:
                            if sup in index and '$' not in sup:
                                gen_level.setdefault(sup, gen_level[top]); load2(sup); pending.append(sup)
                    load2(top); pending.append(top)
            elif miss and not miss[0].startswith(('java', 'javax', 'org.w3c', 'org.xml')):
                end = m.end()
                arity = 0
                if end < len(text) and text[end] == '<':
                    depth, k, arity = 0, end, 1
                    while k < len(text):
                        ch = text[k]
                        if ch == '<': depth += 1
                        elif ch == '>':
                            depth -= 1
                            if depth == 0: break
                        elif ch == ',' and depth == 1: arity += 1
                        k += 1
                key = (miss[0], miss[1])
                placeholders[key] = max(placeholders.get(key, 0), arity)
    n = 0
    for name, src in sorted(srcs.items()):
        pkg = name[1:name.rindex('/')].replace('/', '.') if '/' in name else ''
        path = os.path.join(out, name[1:-1] + '.java')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as fh:
            if pkg: fh.write('package %s;\n\n' % pkg)
            fh.write(src + '\n')
        n += 1
    gen_names = {name[1:-1].replace('/', '.') for name in srcs}
    gp = 0
    for (pkg, cname), arity in sorted(placeholders.items()):
        if (pkg + '.' + cname) in gen_names: continue
        path = os.path.join(out, pkg.replace('.', '/'), cname + '.java')
        if os.path.exists(path): continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tp = ('<' + ', '.join('T%d' % i for i in range(arity)) + '>') if arity else ''
        ext = ' extends Exception' if cname in throwing or (pkg + '.' + cname) in throwing else ''
        with open(path, 'w') as fh:
            fh.write('package %s;\n\npublic class %s%s%s {\n    public %s() {}\n}\n' % (pkg, cname, tp, ext, cname))
        gp += 1
    print('classes carregadas:', len(cl), ' stubs:', n, ' placeholders:', gp, file=sys.stderr)


if __name__ == '__main__':
    main()
