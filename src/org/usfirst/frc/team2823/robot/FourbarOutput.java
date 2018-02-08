package org.usfirst.frc.team2823.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;

import edu.wpi.first.wpilibj.PIDOutput;

public class FourbarOutput implements PIDOutput {
	
	Robot r;
	public FourbarOutput()  {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void pidWrite(double output) {
		// TODO Auto-generated method stub
		r.fourbarMotor.set(ControlMode.PercentOutput, output);
		
	}

}
