#!/usr/bin/env python3
"""Gera classes "sombra" a partir dos stubs: metodos listados recebem implementacao do simulador; os demais deixam de
lancar excecao e passam a devolver valor padrao (registrando o uso em Dyn.unsupported)."""
import re, sys, os

LINE = re.compile(r'^(\s*)((?:public |protected |static |final |abstract |default )*)(<[^>]*> )?([\w.\[\]<>?, ]+?) (\w+)\((.*)\)((?: throws [\w., ]+)?) \{ throw new UnsupportedOperationException\(\); \}\s*$')


def argc(params):
    params = params.strip()
    if not params: return 0
    depth = n = 0
    n = 1
    for ch in params:
        if ch == '<': depth += 1
        elif ch == '>': depth -= 1
        elif ch == ',' and depth == 0: n += 1
    return n


def default_ret(ret):
    ret = ret.strip()
    return {'void': None, 'boolean': 'false', 'int': '0', 'long': '0L', 'float': '0f', 'double': '0.0',
            'short': '(short) 0', 'byte': '(byte) 0', 'char': "'\\0'"}.get(ret, 'null')


def shadow(stub, out, cls, patches):
    lines = open(stub).read().split('\n')
    res = []
    for ln in lines:
        m = LINE.match(ln)
        if not m:
            res.append(ln); continue
        ind, mods, tp, ret, name, params, thr, = m.groups()
        key = '%s/%d' % (name, argc(params))
        if 'static' in mods and 'final' not in mods: pass
        body = patches.get(key)
        d = default_ret(ret)
        if body is None:
            body = 'biobuzz.sdk.Dyn.unsupported("%s.%s");%s' % (cls, name, (' return %s;' % d) if d else '')
        res.append('%s%s%s%s %s(%s)%s { %s }' % (ind, mods, tp or '', ret, name, params, thr, body))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, 'w').write('\n'.join(res))


S = os.environ.get('STUBS_SRC', 'stubs_src') + '/'
O = os.environ.get('SIM_SRC', 'src/main/java') + '/'
PP = 'biobuzz.sdk.SimPinpoint'
shadow(S + 'com/qualcomm/hardware/gobilda/GoBildaPinpointDriver.java', O + 'com/qualcomm/hardware/gobilda/GoBildaPinpointDriver.java',
       'GoBildaPinpointDriver', {
    'update/0': PP + '.update();', 'update/1': PP + '.update();',
    'getPosX/1': 'return %s.posX(p0.name());' % PP, 'getPosY/1': 'return %s.posY(p0.name());' % PP,
    'getHeading/1': 'return %s.heading(p0.name());' % PP,
    'getVelX/1': 'return %s.velX(p0.name());' % PP, 'getVelY/1': 'return %s.velY(p0.name());' % PP,
    'getHeadingVelocity/1': 'return %s.headingVelocity(p0.name());' % PP,
    'setHeading/2': '%s.setHeading(p0, p1.name());' % PP,
    'setPosX/2': '%s.setPosX(p0, p1.name());' % PP, 'setPosY/2': '%s.setPosY(p0, p1.name());' % PP,
    'resetPosAndIMU/0': PP + '.reset();', 'recalibrateIMU/0': PP + '.recalibrate();',
    'getPosition/0': 'return new org.firstinspires.ftc.robotcore.external.navigation.Pose2D(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.INCH, %s.posX("INCH"), %s.posY("INCH"), org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS, %s.heading("RADIANS"));' % (PP, PP, PP),
    'setOffsets/3': PP + '.setOffsets(p0, p1, p2.name());',
    'setEncoderResolution/1': '', 'setEncoderResolution/2': '', 'setEncoderDirections/2': '',
    'setYawScalar/1': '', 'setBulkReadScope/1': '', 'setErrorDetectionType/1': '',
    'getDeviceName/0': 'return "goBILDA Pinpoint Odometry Computer (sim)";', 'getFrequency/0': 'return 1000.0;',
})
T = 'biobuzz.sdk.SimTelemetry'
shadow(S + 'com/acmerobotics/dashboard/telemetry/MultipleTelemetry.java', O + 'com/acmerobotics/dashboard/telemetry/MultipleTelemetry.java',
       'MultipleTelemetry', {
    'addData/2': 'return %s.addData(p0, p1);' % T,
    'addData/3': 'return %s.addDataFormat(p0, p1, p2);' % T,
    'addLine/1': 'return %s.addLine(p0);' % T,
    'addLine/0': 'return %s.addLine("");' % T,
    'update/0': 'return %s.update();' % T,
    'clear/0': T + '.clear();', 'clearAll/0': T + '.clear();',
    'setAutoClear/1': '', 'setMsTransmissionInterval/1': '', 'setDisplayFormat/1': '', 'setCaptionValueSeparator/1': '',
})
print('ok')
