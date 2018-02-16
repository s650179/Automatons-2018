/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team2823.robot;

import java.util.ArrayList;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.properties file in the
 * project.
 */
public class Robot extends IterativeRobot {

	Joystick driveStick;
	Joystick operatorStick;
	
	final int xButton = 1;
	final int aButton = 2;
	final int bButton = 3;
	final int yButton = 4;
	final int leftBumper = 5;
	final int rightBumper = 6;
	final int leftTrigger = 7;
	final int rightTrigger = 8;
	
	Button intakeOpenButton;
	Button toggleIntakeDftButton; 
	Button intakeOutButton;
	Button clampButton;
	Button gearHighButton;
	Button gearLowButton;
	
	Button testButton;

	TalonSRX leftMotor1;
	TalonSRX leftMotor2;
	TalonSRX rightMotor3;
	TalonSRX rightMotor4;
	
	TalonSRX leftElbow;
	VictorSPX rightElbow;
	VictorSPX leftBelt;
	VictorSPX rightBelt;
	
	TalonSRX fourbarMotor;
	VictorSPX elevatorMotor;

	DoubleSolenoid clamper;
	DoubleSolenoid driveSolenoid;
	Compressor compressor;
	
	Encoder lDriveEncoder;
	Encoder rDriveEncoder;
	Encoder lIntakeEncoder;
	Encoder rIntakeEncoder;
	Encoder fourbarEncoder;
	Encoder elevatorEncoder;
	
	ADXRS450_Gyro gyro;
	
	DriveInchesPIDSource leftInches;
	DriveInchesPIDSource rightInches;
	
	
	SnazzyPIDController leftPIDControl;
	SnazzyPIDController rightPIDControl;
	
	SnazzyPIDController leftIntakeControl;
	SnazzyPIDController rightIntakeControl;
	
	final double stow = 0.0;
	final double open = 30.0;
	final double grab = 20.0;
	final double start = 0.0;
	double intakeSetpoint = 0.0;
	String intakeIndicator = "Start";
	
	SnazzyPIDController fourbarPIDControl;
	SnazzyMotionPlanner fourbarMotionControl;
	FourbarOutput fourbarOutput;
	
	final double intake = 0.0;
	final double mid = 10.0;
	final double high_mid = 20.0;
	final double high = 30.0;
	double fourbarSetpoint = 0.0;
	
	SnazzyMotionPlanner leftControl;
	SnazzyMotionPlanner rightControl;
	
	LeftDrivePIDOutput lDriveOutput;
	RightDrivePIDOutput rDriveOutput;
	
	LeftIntakeOutput lIntakeOutput;
	RightIntakeOutput rIntakeOutput;
	
	TrajectoryPlanner rightSwitchAutoTraj;
	TrajectoryPlanner leftSwitchAutoTraj;
	
	TrajectoryPlanner rightScaleAutoTraj;
	
	double[][] rightSwitchAutoPlan = {{0,0,0},{120, -100, 0}};
	double[][] leftSwitchAutoPlan = {{0,0,0},{120, 100, 0}};
	double[][] rightScaleAutoPlan = {{0,0,0},{60, 0, 0},{120,-60, -90}};

	boolean calibrate = false;
	boolean pidTune = false;
	
	SendableChooser<Autonomous> autonomousChooser;
	
	final static double ENC_TO_INCH = Math.PI * 6.0 * (24.0/60.0) * (1.0/3.0) * (1.0/256.0);
	final static double INCH_TO_ENC = 1/ENC_TO_INCH;
	
	/*  1 spin of the wheel corresponds to 60/24 spins of the output axle (that's our 3rd stage gearing).
	 *  The output axle is connected to a 36 t 'encoder gear' which spins another 12t 'encoder gear', so the encoder axles spins at 3 times that rate. 
	 *  Then we have 256 ticks per revolution.  So I think that's a total of 60 / 24 * 3 * 256 = 1920  ticks per revolution
	 */
	
	DoubleSolenoid.Value highGear = DoubleSolenoid.Value.kReverse;
	DoubleSolenoid.Value lowGear = DoubleSolenoid.Value.kForward;
	
	DoubleSolenoid.Value clampIt = DoubleSolenoid.Value.kForward;
	DoubleSolenoid.Value unClampIt = DoubleSolenoid.Value.kReverse;
	
	double transmissionUpper = 50.0;
	double transmissionLower = 30.0;
	
	double velocity = 0.0;
	double rCurrentDist = 0.0;
	double lCurrentDist = 0.0;
	double currentTime = 0.0;
	int velocitySample = 20;
	ArrayList <Double> lLastDistances;
	ArrayList <Double> rLastDistances;
	ArrayList <Double> lastTimes;
	boolean manual = false;
	/**
	 * This function is run when the robot is first started up and should be used
	 * for any initialization code.
	 */
	@Override
	public void robotInit() {
		autonomousChooser = new SendableChooser<Autonomous>();
		autonomousChooser.addDefault("Empty: Do Nothing", new EmptyAutonomous(this));
		autonomousChooser.addObject("Switch Auto", new SwitchAuto(this));
		autonomousChooser.addObject("Scale Auto", new RightScaleAuto(this));
		autonomousChooser.addObject("Do a Spin", new SpinnyAuto(this));
		SmartDashboard.putData("Autonomous Mode", autonomousChooser);
		
		/** UsbCamera c = CameraServer.getInstance().startAutomaticCapture();
	        c.setResolution(320, 180);
	        c.setFPS(29);
	        **/
		driveStick = new Joystick(0);
		operatorStick = new Joystick(1);
		
		lLastDistances = new ArrayList<Double>(velocitySample);
		rLastDistances = new ArrayList<Double>(velocitySample);
		lastTimes = new ArrayList<Double>(velocitySample);

		intakeOpenButton = new Button();
		toggleIntakeDftButton = new Button();
		intakeOutButton = new Button();
		clampButton = new Button();
		gearHighButton = new Button();
		gearLowButton = new Button();
		
		testButton = new Button();
		
		leftMotor1 = new TalonSRX(1);
		leftMotor2 = new TalonSRX(2);
		rightMotor3 = new TalonSRX(3);
		rightMotor4 = new TalonSRX(4);
		
		leftMotor1.configOpenloopRamp(0, 0);
		leftMotor2.configOpenloopRamp(0, 0);
		rightMotor3.configOpenloopRamp(0, 0);
		rightMotor4.configOpenloopRamp(0, 0);
		
		leftElbow = new TalonSRX(11);
		rightElbow = new VictorSPX(12);
		leftBelt = new VictorSPX(21);
		rightBelt = new VictorSPX(22);
		
		fourbarMotor = new TalonSRX(31);
		fourbarOutput = new FourbarOutput(this);
		elevatorMotor = new VictorSPX(41);
			
		lDriveEncoder = new Encoder(0, 1, false, Encoder.EncodingType.k4X);
		lDriveEncoder.setDistancePerPulse(1);
		rDriveEncoder = new Encoder(2, 3, true, Encoder.EncodingType.k4X);
		rDriveEncoder.setDistancePerPulse(1);
		
		lIntakeEncoder = new Encoder(4, 5, false, Encoder.EncodingType.k4X);
		rIntakeEncoder = new Encoder(6, 7, false, Encoder.EncodingType.k4X);
		
		leftInches = new DriveInchesPIDSource(lDriveEncoder);
		rightInches = new DriveInchesPIDSource(rDriveEncoder);
		
		fourbarEncoder = new Encoder(8,9, true, Encoder.EncodingType.k4X);
		elevatorEncoder = new Encoder( 10, 11, false, Encoder.EncodingType.k4X);

		clamper = new DoubleSolenoid(2,3);
		driveSolenoid = new DoubleSolenoid(0, 1);
		compressor = new Compressor(0);

		driveSolenoid.set(lowGear);
		clamper.set(unClampIt);

		lDriveEncoder.reset();
		rDriveEncoder.reset();
		lIntakeEncoder.reset();
		rIntakeEncoder.reset();
		fourbarEncoder.reset();
		elevatorEncoder.reset();
		
		//gyro = new ADXRS450_Gyro();
		//gyro.reset();
	
		lDriveOutput = new LeftDrivePIDOutput(this);
		rDriveOutput = new RightDrivePIDOutput(this);
		
		lIntakeOutput = new LeftIntakeOutput(this);
		rIntakeOutput = new RightIntakeOutput(this);
		
		rightSwitchAutoTraj = new TrajectoryPlanner(rightSwitchAutoPlan, 100*0.9, 2000*0.5, 3400*0.5); //the integers are what the chassis is capable of, then we limit it with the decimals
		rightSwitchAutoTraj.generate();
		
		leftSwitchAutoTraj = new TrajectoryPlanner(leftSwitchAutoPlan, 100*0.9, 2000*0.5, 3400*0.5);
		leftSwitchAutoTraj.generate();
		
		rightScaleAutoTraj = new TrajectoryPlanner(rightScaleAutoPlan, 100*0.35, 2000*0.4, 3400*0.4);
		rightScaleAutoTraj.generate();
		
		leftControl = new SnazzyMotionPlanner(0.1, 0.001, 0.75, 0, 0.00152, 0.0101,  leftInches, lDriveOutput, 0.005, "Left.csv");
		rightControl= new SnazzyMotionPlanner(0.1, 0.001, 0.75, 0, 0.00152, 0.0101,  rightInches, rDriveOutput, 0.005,"Right.csv");
		
		leftPIDControl = new SnazzyPIDController(0.04, 0.001, 0.8, 0, leftInches, lDriveOutput, 0.005, "Left.csv");
		rightPIDControl= new SnazzyPIDController(0.04, 0.001, 0.8, 0, rightInches, rDriveOutput, 0.005,"Right.csv");
		
		fourbarPIDControl = new SnazzyPIDController(0.0005, 0.0, 0.0, 0.0, fourbarEncoder, fourbarOutput, 0.005, "FourbarPID.csv");
		fourbarPIDControl.setOutputRange(-0.5, 0.8);
		
		fourbarSetpoint = 0;
		fourbarMotionControl = new SnazzyMotionPlanner(0.0005, 0.0, 0.0, 0.0, 0.00000310,0.00000387, fourbarEncoder, fourbarOutput, 0.005, "FourbarMotion.csv");
		
		leftIntakeControl = new SnazzyPIDController(0.05, 0, 0, 0, lIntakeEncoder, lIntakeOutput, 0.005, "leftIntake.csv");
		rightIntakeControl = new SnazzyPIDController(0.05, 0, 0, 0, rIntakeEncoder, rIntakeOutput, 0.005, "rightIntake.csv");
		
		intakeSetpoint = start;
		
		SmartDashboard.putNumber("P", 0.01);
		SmartDashboard.putNumber("I", 0.0);
		SmartDashboard.putNumber("D", 0.0);
		SmartDashboard.putNumber("Setpoint", 0);
		SmartDashboard.putNumber("L Encoder", 0);
		SmartDashboard.putNumber("R Encoder", 0);
		SmartDashboard.putNumber("L Elbow", 0);
		SmartDashboard.putNumber("R Elbow", 0);
		SmartDashboard.putString("Driving Gear", "Low");
		SmartDashboard.putString("Intake Setpoint", "Start");
		SmartDashboard.putNumber("Fourbar", 0);
		
	}

	@Override
	public void autonomousInit() {
		
		lDriveEncoder.reset();
		rDriveEncoder.reset();
		lIntakeEncoder.reset();
		rIntakeEncoder.reset();
		fourbarEncoder.reset();
		elevatorEncoder.reset();
		
		leftControl.reset();
		rightControl.reset();
		
		driveSolenoid.set(lowGear);
		
		((Autonomous) autonomousChooser.getSelected()).init();
		
		
	}

	@Override
	public void autonomousPeriodic() {
		((Autonomous) autonomousChooser.getSelected()).periodic();
		
		SmartDashboard.putNumber("L Encoder", lDriveEncoder.get());
		SmartDashboard.putNumber("R Encoder", rDriveEncoder.get());
	}

	@Override
	public void teleopPeriodic() {
		
		calibrate = false;
		pidTune = false;
		
		if(!calibrate && !pidTune) {			
			gearLowButton.update(driveStick.getRawButton(leftTrigger));
			gearHighButton.update(driveStick.getRawButton(leftBumper));
			intakeOpenButton.update(driveStick.getRawButton(rightBumper));
			toggleIntakeDftButton.update(driveStick.getRawButton(yButton));
			intakeOutButton.update(driveStick.getRawButton(rightTrigger));
			clampButton.update(driveStick.getRawButton(bButton));
			testButton.update(driveStick.getRawButton(1));
			
			currentTime = Timer.getFPGATimestamp();
			rCurrentDist = rightInches.pidGet();
			lCurrentDist = leftInches.pidGet();
			lLastDistances.add(lCurrentDist);
			rLastDistances.add(rCurrentDist);
			lastTimes.add(currentTime);
			
			if(clampButton.on()) {
				clamper.set(clampIt);
			}else if(!clampButton.on()) {
				clamper.set(unClampIt);
			}
	
			if(lLastDistances.size()==velocitySample && rLastDistances.size() == velocitySample ) {
				velocity  = ((lCurrentDist-lLastDistances.get(0))/(currentTime-lastTimes.get(0))+(rCurrentDist-rLastDistances.get(0))/(currentTime-lastTimes.get(0)))/2;
				if(Math.abs(velocity) >= 50.0 && driveSolenoid.get() == lowGear && !manual) {
					driveSolenoid.set(highGear);
					SmartDashboard.putString("Driving Gear", "High");
				} 
				
				if(Math.abs(velocity) <= 30.0 && driveSolenoid.get() == highGear && !manual) {
					driveSolenoid.set(lowGear);
					SmartDashboard.putString("Driving Gear", "Low");
				}
				
				lLastDistances.remove(0);
				rLastDistances.remove(0);
				lastTimes.remove(0);
			}
			
			if(driveStick.getRawButton(4) && fourbarSetpoint <60000 ) {
				fourbarSetpoint +=500;
			}
			if(driveStick.getRawButton(2) && fourbarSetpoint > 0) {
				fourbarSetpoint -= 500;
			}
			
			SmartDashboard.putNumber("Setpoint", fourbarSetpoint);
			fourbarPIDControl.setSetpoint(fourbarSetpoint);
			fourbarPIDControl.enable();
			
			if(toggleIntakeDftButton.on()) {
				
				if(intakeOpenButton.held() && !intakeOutButton.held()) {
					intakeSetpoint = open;
				} else if(!intakeOpenButton.held() && intakeOutButton.held()) {
					intakeSetpoint = grab;
					
				}else {
					intakeSetpoint = grab;
				}
				
			} else if(!toggleIntakeDftButton.on()) {
				if(intakeOpenButton.held() && !intakeOutButton.held()) {
					intakeSetpoint = open;
				} else if(!intakeOpenButton.held() && intakeOutButton.held()) {
					intakeSetpoint = grab;
					
				}else {
					intakeSetpoint = stow;
				}
			}
			
			
			
			if(intakeSetpoint >= grab && !intakeOutButton.held()) {
				leftBelt.set(ControlMode.PercentOutput, 1.0);
				rightBelt.set(ControlMode.PercentOutput, 1.0);
			} else if(intakeOutButton.held()) {
				leftBelt.set(ControlMode.PercentOutput, -1.0);
				rightBelt.set(ControlMode.PercentOutput, -1.0);
				
			}else {
				leftBelt.set(ControlMode.PercentOutput, 0.0);
				rightBelt.set(ControlMode.PercentOutput, 0.0);
			}
			
			leftIntakeControl.setSetpoint(intakeSetpoint);
			leftIntakeControl.enable();
			
			rightIntakeControl.setSetpoint(intakeSetpoint);
			rightIntakeControl.enable();
			
			/*if(testButton.on()){
				if(testButton.changed()) {
					fourbarEncoder.reset();
				
					fourbarMotionControl.configureGoal(60000, 130000*0.5, 3000000*0.5, false);
					fourbarMotionControl.enable();
					
				}
				
			}else if (testButton.changed()&& !testButton.on()){	
				fourbarMotionControl.disable();
				
			}*/
	
			if (gearLowButton.held()) {
				manual = true;
				driveSolenoid.set(lowGear);
				SmartDashboard.putString("Driving Gear", "Low");
				
			}else if (gearHighButton.held()) {
				manual = true;
				driveSolenoid.set(highGear);
				SmartDashboard.putString("Driving Gear", "High");
				
			} else {
				manual = false;
			}
			
			SmartDashboard.putNumber("L Encoder", leftInches.pidGet());
			SmartDashboard.putNumber("R Encoder", rightInches.pidGet());
			SmartDashboard.putNumber("L Elbow", lIntakeEncoder.get());
			SmartDashboard.putNumber("R Elbow", rIntakeEncoder.get());
			SmartDashboard.putNumber("Fourbar", fourbarEncoder.get());
			
			switch((int)intakeSetpoint) {
				case (int)grab:
					intakeIndicator = "Grab";
					break;
				case (int)open:
					intakeIndicator = "Open";
					break;
				case (int)stow:
					intakeIndicator = "Stow";
					break;
					
			}
					
					
			SmartDashboard.putString("Intake Setpoint", intakeIndicator);

			
			leftMotor1.set(ControlMode.PercentOutput, Math.pow(driveStick.getRawAxis(1), 3) );
			leftMotor2.set(ControlMode.PercentOutput, Math.pow(driveStick.getRawAxis(1), 3) );
			rightMotor3.set(ControlMode.PercentOutput, -Math.pow(driveStick.getRawAxis(3), 3));
			rightMotor4.set(ControlMode.PercentOutput, -Math.pow(driveStick.getRawAxis(3), 3));
			
			elevatorMotor.set(ControlMode.PercentOutput, operatorStick.getRawAxis(3)*.4);
			
		
		} else if(calibrate && !pidTune) {
			calibrateNow();
		}else if(!calibrate && pidTune) {
			pidTune();
		}
		
	}


	@Override
	public void testInit() {
		leftControl.disable();
		rightControl.disable();
	}

	@Override
	public void testPeriodic() {
		
	}
	public void disabledInit() {
		
	}
	
	public void calibrateNow() {
		testButton.update(driveStick.getRawButton(1));
		
		if(testButton.on()){
			if(testButton.changed()) {
				lDriveEncoder.reset();
				rDriveEncoder.reset();
				
				//leftControl.startCalibration();
				//rightControl.startCalibration();
				
				//leftControl.enable();
				//rightControl.enable();
				
				fourbarMotionControl.configureGoal(60000, 130000*0.5, 3000000*0.5, false);
				fourbarMotionControl.enable();
				
			}
			
		}else if (testButton.changed()&& !testButton.on()){
			//leftControl.disable();
			//rightControl.disable();		
			fourbarMotionControl.disable();
			
		}
		SmartDashboard.putNumber("L Encoder", leftInches.pidGet());
		SmartDashboard.putNumber("R Encoder", rightInches.pidGet());
	}
	
	public void pidTune() {
		testButton.update(driveStick.getRawButton(1));
		
		leftPIDControl.setPID(SmartDashboard.getNumber("P", 0), SmartDashboard.getNumber("I", 0), SmartDashboard.getNumber("D", 0));
		rightPIDControl.setPID(SmartDashboard.getNumber("P", 0), SmartDashboard.getNumber("I", 0), SmartDashboard.getNumber("D", 0));
		fourbarPIDControl.setPID(SmartDashboard.getNumber("P", 0),SmartDashboard.getNumber("I", 0), SmartDashboard.getNumber("D", 0));
		leftIntakeControl.setPID(SmartDashboard.getNumber("P", 0), SmartDashboard.getNumber("I", 0), SmartDashboard.getNumber("D", 0));
		rightIntakeControl.setPID(SmartDashboard.getNumber("P", 0), SmartDashboard.getNumber("I", 0), SmartDashboard.getNumber("D", 0));
	
		if(testButton.on()){
				if(testButton.changed()) {
					lDriveEncoder.reset();
					rDriveEncoder.reset();
					
					fourbarEncoder.reset();
					
					lIntakeEncoder.reset();
					
					//leftPIDControl.setSetpoint(SmartDashboard.getNumber("Setpoint", 0));
					//leftPIDControl.enable();
					
					leftIntakeControl.setSetpoint(SmartDashboard.getNumber("Setpoint", 0));
					leftIntakeControl.enable();
					
					//rightIntakeControl.setSetpoint(SmartDashboard.getNumber("Setpoint", 0));
					//rightIntakeControl.enable();
					
					//fourbarPIDControl.setSetpoint(SmartDashboard.getNumber("Setpoint", 0));
					//fourbarPIDControl.enable();
					
					//rightPIDControl.setSetpoint(SmartDashboard.getNumber("Setpoint", 0));
					//rightPIDControl.enable();
	
				}
				
			}else if (testButton.changed()&& !testButton.on()){
				//leftPIDControl.disable();
				//rightPIDControl.disable();
				//fourbarPIDControl.disable();
				leftIntakeControl.disable();
				//rightIntakeControl.disable();
				
			}
		SmartDashboard.putNumber("L Encoder", leftInches.pidGet());
		SmartDashboard.putNumber("R Encoder", rightInches.pidGet());
		SmartDashboard.putNumber("Fourbar", fourbarEncoder.get());
		SmartDashboard.putNumber("L Elbow", lIntakeEncoder.get());
		SmartDashboard.putNumber("R Elbow", rIntakeEncoder.get());
	}
	
	
}
