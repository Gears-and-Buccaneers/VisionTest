/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.*;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SpeedControllerGroup;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.GenericHID.Hand;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  private boolean driverVision, tapeVision, cargoVision, cargoSeen, tapeSeen;
  private NetworkTableEntry tapeDetected, cargoDetected, tapeYaw, cargoYaw, cargoWanted, driveWanted, tapeWanted, videoTimestamp;
  private Joystick driverJoy;
  private double targetAngle;
  NetworkTableInstance instance;
  NetworkTable chickenVision;
  /*** EDIT TO CURRENT CONFIGURATION ***/
  DifferentialDrive drive;
  Hand leftSide, rightSide;

  WPI_TalonSRX leftFrontTalon = null;
	WPI_TalonSRX leftBackTalon = null;
	WPI_TalonSRX rightFrontTalon = null;
	WPI_TalonSRX rightBackTalon = null;


  /**
   * This function is run when the robot is first started up and should be
   * used for any initialization code.s
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    instance = NetworkTableInstance.getDefault();
 
    chickenVision = instance.getTable("ChickenVision");

    tapeDetected = chickenVision.getEntry("tapeDetected");
    cargoDetected = chickenVision.getEntry("cargoDetected");
    tapeYaw = chickenVision.getEntry("tapeYaw");
    cargoYaw = chickenVision.getEntry("cargoYaw");

    driveWanted = chickenVision.getEntry("Driver");
    tapeWanted = chickenVision.getEntry("Tape");
    cargoWanted = chickenVision.getEntry("Cargo");

    videoTimestamp = chickenVision.getEntry("VideoTimestamp");

    tapeVision = cargoVision = false;
    driverVision = true;
    driverJoy = new Joystick(0);
    /*** EDIT TO CURRENT CONFIGURATION ***/
    leftFrontTalon = new WPI_TalonSRX(0);
    leftBackTalon = new WPI_TalonSRX(1);
    rightFrontTalon = new WPI_TalonSRX(3);
    rightBackTalon = new WPI_TalonSRX(7);

    SpeedControllerGroup leftMotors = new SpeedControllerGroup(leftFrontTalon, leftBackTalon);
		SpeedControllerGroup rightMotors = new SpeedControllerGroup(rightFrontTalon, rightBackTalon);

    drive = new DifferentialDrive(leftMotors, rightMotors);
        
    leftSide = Hand.kLeft;
    rightSide = Hand.kRight;
    targetAngle = 0;
    /*** Update Dashboard ***/

  }

  /**
   * This function is called every robot packet, no matter the mode. Use
   * this for items like diagnostics that you want ran during disabled,
   * autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before
   * LiveWindow and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable
   * chooser code works with the Java SmartDashboard. If you prefer the
   * LabVIEW Dashboard, remove all of the chooser code and uncomment the
   * getString line to get the auto name from the text box below the Gyro
   *
   * <p>You can add additional auto modes by adding additional comparisons to
   * the switch structure below with additional strings. If using the
   * SendableChooser make sure to add them to the chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      case kCustomAuto:
        // Put custom auto code here
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here
        break;
    }
  }

    /**
     * This function is called periodically during operator control Arcade drive
     * Left Y controls forward and back Right X controls rotation
     * 
     * If want to auto align with cargo, you hold down left bumper
     * 
     * If want to auto align with vision tapes, you hold down right bumper
     * 
     * You still have forward and backwards control no matter what button you press
     */
    @Override
    public void teleopPeriodic() {
        SmartDashboard.putNumber("cargo yaw", (cargoYaw.getDouble(0)));
        // Change this to alter how quick or slow the feedback loop is
        double kP = 0.2;
 
        double forward = driverJoy.getY(leftSide);
        double turn = driverJoy.getX(rightSide);
 
        boolean cargoDesired = driverJoy.getRawButton(5);
        boolean tapeDesired = driverJoy.getRawButton(6);
        // If button 1 is pressed, then it will track cargo
        if (cargoDesired) {
 
            driveWanted.setBoolean(false);
            tapeWanted.setBoolean(false);
            cargoWanted.setBoolean(true);
            cargoSeen = cargoDetected.getBoolean(false);
 
            if (cargoSeen)
                targetAngle = cargoYaw.getDouble(0);
            else
                targetAngle = 0;
 
        } else if (tapeDesired) {
 
 
            driveWanted.setBoolean(false);
            tapeWanted.setBoolean(true);
            cargoWanted.setBoolean(false);
            // Checks if vision sees cargo or vision targets. This may not get called unless
            // cargo vision detected
            tapeSeen = tapeDetected.getBoolean(false);
 
            if (tapeSeen)
                targetAngle = tapeYaw.getDouble(0);
            else
                targetAngle = 0;
 
        } else {
 
 
            driveWanted.setBoolean(true);
            tapeWanted.setBoolean(false);
            cargoWanted.setBoolean(false);
 
            targetAngle = 0;
 
        }
        // Limit output to 0.3
        double output = limitOutput(-kP * -targetAngle, 0.8);
 
        if (cargoDesired || tapeDesired)
            drive.arcadeDrive(forward, output);
        else
            drive.arcadeDrive(forward, turn);
 
    }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }
  public double limitOutput(double number, double maxOutput) {
    if (number > 1.0) {
        number = 1.0;
    }
    if (number < -1.0) {
        number = -1.0;
    }

    if (number > maxOutput) {
        return maxOutput;
    }
    if (number < -maxOutput) {
        return -maxOutput;
    }

    return number;
}

}
