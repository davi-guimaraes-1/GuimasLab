package com.acmerobotics.dashboard.telemetry;

public class MultipleTelemetry implements org.firstinspires.ftc.robotcore.external.Telemetry {
    public MultipleTelemetry(org.firstinspires.ftc.robotcore.external.Telemetry... p0) {  }
    public java.lang.Object addAction(java.lang.Runnable p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.addAction"); return null; }
    public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.Object p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
    public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, org.firstinspires.ftc.robotcore.external.Func<T> p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
    public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, java.lang.Object... p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
    public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, org.firstinspires.ftc.robotcore.external.Func<T> p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
    public org.firstinspires.ftc.robotcore.external.Telemetry.Line addLine() { return biobuzz.sdk.SimTelemetry.addLine(""); }
    public org.firstinspires.ftc.robotcore.external.Telemetry.Line addLine(java.lang.String p0) { return biobuzz.sdk.SimTelemetry.addLine(p0); }
    public void addTelemetry(org.firstinspires.ftc.robotcore.external.Telemetry p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.addTelemetry"); }
    public void clear() { biobuzz.sdk.SimTelemetry.clear(); }
    public void clearAll() { biobuzz.sdk.SimTelemetry.clear(); }
    public java.lang.String getCaptionValueSeparator() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getCaptionValueSeparator"); return null; }
    public java.lang.String getItemSeparator() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getItemSeparator"); return null; }
    public int getMsTransmissionInterval() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getMsTransmissionInterval"); return 0; }
    public boolean isAutoClear() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.isAutoClear"); return false; }
    public org.firstinspires.ftc.robotcore.external.Telemetry.Log log() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.log"); return null; }
    public boolean removeAction(java.lang.Object p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.removeAction"); return false; }
    public boolean removeItem(org.firstinspires.ftc.robotcore.external.Telemetry.Item p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.removeItem"); return false; }
    public boolean removeLine(org.firstinspires.ftc.robotcore.external.Telemetry.Line p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.removeLine"); return false; }
    public void setAutoClear(boolean p0) {  }
    public void setCaptionValueSeparator(java.lang.String p0) {  }
    public void setDisplayFormat(org.firstinspires.ftc.robotcore.external.Telemetry.DisplayFormat p0) {  }
    public void setItemSeparator(java.lang.String p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setItemSeparator"); }
    public void setMsTransmissionInterval(int p0) {  }
    public void speak(java.lang.String p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.speak"); }
    public void speak(java.lang.String p0, java.lang.String p1, java.lang.String p2) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.speak"); }
    public boolean update() { return biobuzz.sdk.SimTelemetry.update(); }
    public class MultipleItem implements org.firstinspires.ftc.robotcore.external.Telemetry.Item {
        public MultipleItem(java.util.List<org.firstinspires.ftc.robotcore.external.Telemetry.Item> p0) {  }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.Object p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, org.firstinspires.ftc.robotcore.external.Func<T> p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, java.lang.Object... p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, org.firstinspires.ftc.robotcore.external.Func<T> p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
        public java.lang.String getCaption() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getCaption"); return null; }
        public boolean isRetained() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.isRetained"); return false; }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item setCaption(java.lang.String p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setCaption"); return null; }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item setRetained(java.lang.Boolean p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setRetained"); return null; }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item setValue(java.lang.Object p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setValue"); return null; }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item setValue(java.lang.String p0, org.firstinspires.ftc.robotcore.external.Func<T> p1) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setValue"); return null; }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item setValue(java.lang.String p0, java.lang.Object... p1) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setValue"); return null; }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item setValue(org.firstinspires.ftc.robotcore.external.Func<T> p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setValue"); return null; }
    }
    public class MultipleLine implements org.firstinspires.ftc.robotcore.external.Telemetry.Line {
        public MultipleLine(java.util.List<org.firstinspires.ftc.robotcore.external.Telemetry.Line> p0) {  }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.Object p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, org.firstinspires.ftc.robotcore.external.Func<T> p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, java.lang.String p1, java.lang.Object... p2) { return biobuzz.sdk.SimTelemetry.addDataFormat(p0, p1, p2); }
        public <T> org.firstinspires.ftc.robotcore.external.Telemetry.Item addData(java.lang.String p0, org.firstinspires.ftc.robotcore.external.Func<T> p1) { return biobuzz.sdk.SimTelemetry.addData(p0, p1); }
    }
    public class MultipleLog implements org.firstinspires.ftc.robotcore.external.Telemetry.Log {
        public MultipleLog(com.acmerobotics.dashboard.telemetry.MultipleTelemetry p0) {  }
        public void add(java.lang.String p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.add"); }
        public void add(java.lang.String p0, java.lang.Object... p1) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.add"); }
        public void addLog(org.firstinspires.ftc.robotcore.external.Telemetry.Log p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.addLog"); }
        public void clear() { biobuzz.sdk.SimTelemetry.clear(); }
        public int getCapacity() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getCapacity"); return 0; }
        public org.firstinspires.ftc.robotcore.external.Telemetry.Log.DisplayOrder getDisplayOrder() { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.getDisplayOrder"); return null; }
        public void setCapacity(int p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setCapacity"); }
        public void setDisplayOrder(org.firstinspires.ftc.robotcore.external.Telemetry.Log.DisplayOrder p0) { biobuzz.sdk.Dyn.unsupported("MultipleTelemetry.setDisplayOrder"); }
    }
}
