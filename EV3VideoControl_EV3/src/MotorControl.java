import lejos.hardware.motor.UnregulatedMotor;
import lejos.hardware.port.MotorPort;
import java.lang.Math;

public class MotorControl {
	private UnregulatedMotor leftMotor = null;
	private UnregulatedMotor rightMotor = null;
	private static int MAX_POWER = 50; // maximum engine power

	public MotorControl()
	{
		// init
		leftMotor = new UnregulatedMotor(MotorPort.A);
		rightMotor = new UnregulatedMotor(MotorPort.D);
		
		freeMotors();
	}
	
	void freeMotors()
	{
		leftMotor.flt();
		rightMotor.flt();
	}
	
	public void executeCommand(int command) // command -- degrees, in which the robot should move (in a coordinate system ; unit circle)
	{
		System.out.println("cmd recv: " + command);
		// received command to shut down engines ?
		if (command < 0)
		{
			freeMotors();
			return;
		}
		
		double vertical = Math.sin(Math.PI/180 * command); // "vertical power"
		
		int leftPower, rightPower = (int)Math.round(Math.abs(vertical)*(double)MAX_POWER);
		leftPower = rightPower;
		
		// go to 2nd or 3rd quadrant === left ?
		if (command >= 90 && command <= 270)
			rightPower = MAX_POWER;
		else // go to 1st or 4th quadrant === right
			leftPower = MAX_POWER;
		
		// forwards ?
		if (vertical >= 0)
		{
			leftMotor.forward();
			leftMotor.setPower(leftPower);
			rightMotor.forward();
			rightMotor.setPower(rightPower);
		}
		else // backwards
		{
			leftMotor.backward();
			leftMotor.setPower(leftPower);
			rightMotor.backward();
			rightMotor.setPower(rightPower);
		}
		
	}
}
