package biobuzz.sdk.demo;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/** OpMode de exemplo usando os mesmos nomes de hardware da equipe (serve de teste da camada SDK). */
public class DemoTeleOp extends LinearOpMode {
    @Override
    public void runOpMode() {
        DcMotorEx fl = hardwareMap.get(DcMotorEx.class, "superiorEsquerdo");
        DcMotorEx fr = hardwareMap.get(DcMotorEx.class, "superiorDireito");
        DcMotorEx bl = hardwareMap.get(DcMotorEx.class, "inferiorEsquerdo");
        DcMotorEx br = hardwareMap.get(DcMotorEx.class, "inferiorDireito");
        DcMotorEx shooter = hardwareMap.get(DcMotorEx.class, "shooter");
        DcMotorEx intake = hardwareMap.get(DcMotorEx.class, "intake");
        DcMotorEx transfer = hardwareMap.get(DcMotorEx.class, "transfer");
        DcMotorEx torre = hardwareMap.get(DcMotorEx.class, "torre");
        Servo servo = hardwareMap.get(Servo.class, "servo");
        Servo servo2 = hardwareMap.get(Servo.class, "servo2");
        GoBildaPinpointDriver pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        fr.setDirection(DcMotorSimple.Direction.REVERSE);
        torre.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        servo2.setDirection(Servo.Direction.REVERSE);
        pinpoint.resetPosAndIMU();
        waitForStart();
        while (opModeIsActive()) {
            pinpoint.update();
            double y = -gamepad1.left_stick_y, x = gamepad1.left_stick_x, rx = gamepad1.right_stick_x;
            fl.setPower(y + x + rx);
            fr.setPower(y - x - rx);
            bl.setPower(y - x + rx);
            br.setPower(y + x - rx);
            shooter.setVelocity(gamepad1.a ? 1210 : 0);
            transfer.setPower(gamepad1.b ? 1 : 0);
            intake.setPower(gamepad1.x ? 0.7 : 0);
            torre.setPower(gamepad1.dpad_left ? -0.5 : gamepad1.dpad_right ? 0.5 : 0);
            double s = gamepad1.dpad_up ? 1 : 0;
            servo.setPosition(s);
            servo2.setPosition(s);
            telemetry.addData("x", "%.1f", pinpoint.getPosX(DistanceUnit.INCH));
            telemetry.addData("y", "%.1f", pinpoint.getPosY(DistanceUnit.INCH));
            telemetry.addData("heading", "%.0f", pinpoint.getHeading(AngleUnit.DEGREES));
            telemetry.addData("shooter ticks/s", "%.0f", shooter.getVelocity());
            telemetry.addData("torre ticks", torre.getCurrentPosition());
            telemetry.update();
            sleep(10);
        }
    }
}
