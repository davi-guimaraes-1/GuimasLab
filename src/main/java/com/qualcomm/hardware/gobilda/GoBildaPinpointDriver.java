package com.qualcomm.hardware.gobilda;

public class GoBildaPinpointDriver extends com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice<com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple> {
    public GoBildaPinpointDriver(com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple p0, boolean p1) { super(null, false); }
    protected boolean doInitialize() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.doInitialize"); return false; }
    public int getDeviceID() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getDeviceID"); return 0; }
    public java.lang.String getDeviceName() { return "goBILDA Pinpoint Odometry Computer (sim)"; }
    public com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.DeviceStatus getDeviceStatus() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getDeviceStatus"); return null; }
    public int getDeviceVersion() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getDeviceVersion"); return 0; }
    public int getEncoderX() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getEncoderX"); return 0; }
    public int getEncoderY() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getEncoderY"); return 0; }
    public double getFrequency() { return 1000.0; }
    public double getHeading(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit p0) { return biobuzz.sdk.SimPinpoint.heading(p0.name()); }
    public double getHeading(org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit p0) { return biobuzz.sdk.SimPinpoint.heading(p0.name()); }
    public double getHeadingVelocity(org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit p0) { return biobuzz.sdk.SimPinpoint.headingVelocity(p0.name()); }
    public int getLoopTime() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getLoopTime"); return 0; }
    public com.qualcomm.robotcore.hardware.HardwareDevice.Manufacturer getManufacturer() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getManufacturer"); return null; }
    public double getPitch(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit p0) { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getPitch"); return 0.0; }
    public double getPosX(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { return biobuzz.sdk.SimPinpoint.posX(p0.name()); }
    public double getPosY(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { return biobuzz.sdk.SimPinpoint.posY(p0.name()); }
    public org.firstinspires.ftc.robotcore.external.navigation.Pose2D getPosition() { return new org.firstinspires.ftc.robotcore.external.navigation.Pose2D(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.INCH, biobuzz.sdk.SimPinpoint.posX("INCH"), biobuzz.sdk.SimPinpoint.posY("INCH"), org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS, biobuzz.sdk.SimPinpoint.heading("RADIANS")); }
    public org.firstinspires.ftc.robotcore.external.navigation.Quaternion getQuaternion() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getQuaternion"); return null; }
    public double getRoll(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit p0) { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getRoll"); return 0.0; }
    public double getVelX(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { return biobuzz.sdk.SimPinpoint.velX(p0.name()); }
    public double getVelY(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { return biobuzz.sdk.SimPinpoint.velY(p0.name()); }
    public float getXOffset(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getXOffset"); return 0f; }
    public float getYOffset(org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p0) { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getYOffset"); return 0f; }
    public float getYawScalar() { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.getYawScalar"); return 0f; }
    public void recalibrateIMU() { biobuzz.sdk.SimPinpoint.recalibrate(); }
    public void resetPosAndIMU() { biobuzz.sdk.SimPinpoint.reset(); }
    public void setBulkReadScope(com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.Register... p0) {  }
    public void setEncoderDirections(com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.EncoderDirection p0, com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.EncoderDirection p1) {  }
    public void setEncoderResolution(double p0, org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p1) {  }
    public void setEncoderResolution(com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.GoBildaOdometryPods p0) {  }
    public void setErrorDetectionType(com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.ErrorDetectionType p0) {  }
    public void setHeading(double p0, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit p1) { biobuzz.sdk.SimPinpoint.setHeading(p0, p1.name()); }
    public void setOffsets(double p0, double p1, org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p2) { biobuzz.sdk.SimPinpoint.setOffsets(p0, p1, p2.name()); }
    public void setPosX(double p0, org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p1) { biobuzz.sdk.SimPinpoint.setPosX(p0, p1.name()); }
    public void setPosY(double p0, org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit p1) { biobuzz.sdk.SimPinpoint.setPosY(p0, p1.name()); }
    public void setPosition(org.firstinspires.ftc.robotcore.external.navigation.Pose2D p0) { biobuzz.sdk.Dyn.unsupported("GoBildaPinpointDriver.setPosition"); }
    public void setYawScalar(double p0) {  }
    public void update() { biobuzz.sdk.SimPinpoint.update(); }
    public void update(com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.ReadData p0) { biobuzz.sdk.SimPinpoint.update(); }
    public static enum DeviceStatus {
        CALIBRATING, FAULT_BAD_READ, FAULT_IMU_RUNAWAY, FAULT_NO_PODS_DETECTED, FAULT_X_POD_NOT_DETECTED, FAULT_Y_POD_NOT_DETECTED, NOT_READY, READY;
    }
    public static enum EncoderDirection {
        FORWARD, REVERSED;
    }
    public static enum ErrorDetectionType {
        CRC, LOCAL_TEST, NONE;
    }
    public static enum GoBildaOdometryPods {
        goBILDA_4_BAR_POD, goBILDA_SWINGARM_POD;
    }
    public static enum ReadData {
        ONLY_UPDATE_HEADING;
    }
    public static enum Register {
        BULK_READ, DEVICE_CONTROL, DEVICE_ID, DEVICE_STATUS, DEVICE_VERSION, H_ORIENTATION, H_VELOCITY, LOOP_TIME, MM_PER_TICK, PITCH, QUATERNION_W, QUATERNION_X, QUATERNION_Y, QUATERNION_Z, ROLL, SET_BULK_READ, X_ENCODER_VALUE, X_POD_OFFSET, X_POSITION, X_VELOCITY, YAW_SCALAR, Y_ENCODER_VALUE, Y_POD_OFFSET, Y_POSITION, Y_VELOCITY;
    }
}
