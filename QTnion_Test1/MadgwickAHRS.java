package com.example.qtniontest;

/**
 * Minimal Madgwick AHRS (no magnetometer) for Android.
 * Reference:
 *   S. O. H. Madgwick, "An efficient orientation filter for inertial and inertial/magnetic sensor arrays," 2010.
 *
 * Inputs:
 *   - Gyroscope (rad/s)
 *   - Accelerometer (m/s^2)
 *
 * Output:
 *   - Unit quaternion [w, x, y, z] representing device->world orientation
 *
 * Note:
 *   Without magnetometer, yaw will drift; pitch/roll stabilize via gravity.
 */
public class MadgwickAHRS {

  private double beta = 0.1; // gain (tune 0.05~0.2)
  private double q0 = 1, q1 = 0, q2 = 0, q3 = 0; // quaternion

  public MadgwickAHRS(float beta) {
    this.beta = beta;
  }

  public void setBeta(double beta) { this.beta = beta; }

  public double[] getQuaternion() {
    return new double[]{q0, q1, q2, q3};
  }

  public void reset() {
    q0 = 1; q1 = q2 = q3 = 0;
  }

  public void update(float gx, float gy, float gz,
                     float ax, float ay, float az,
                     float dt) {
    // Normalize accelerometer
    double norm = Math.sqrt(ax*ax + ay*ay + az*az);
    if (norm < 1e-6) {
      // invalid accel — integrate gyro only
      integrateGyro(gx, gy, gz, dt);
      return;
    }
    ax /= norm; ay /= norm; az /= norm;

    // Auxiliary variables to avoid repeated arithmetic
    double _2q0 = 2.0*q0;
    double _2q1 = 2.0*q1;
    double _2q2 = 2.0*q2;
    double _2q3 = 2.0*q3;
    double _4q0 = 4.0*q0;
    double _4q1 = 4.0*q1;
    double _4q2 = 4.0*q2;
    double _8q1 = 8.0*q1;
    double _8q2 = 8.0*q2;
    double q0q0 = q0*q0;
    double q1q1 = q1*q1;
    double q2q2 = q2*q2;
    double q3q3 = q3*q3;

    // Gradient decent algorithm corrective step
    double s0 = _4q0*q2q2 + _2q2*ax + _4q0*q1q1 - _2q1*ay;
    double s1 = _4q1*q3q3 - _2q3*ax + 4.0*q0q0*q1 - _2q0*ay - _4q1 + _8q1*q1q1 + _8q1*q2q2 + _4q1*az;
    double s2 = 4.0*q0q0*q2 + _2q0*ax + _4q2*q3q3 - _2q3*ay - _4q2 + _8q2*q1q1 + _8q2*q2q2 + _4q2*az;
    double s3 = 4.0*q1q1*q3 - _2q1*ax + 4.0*q2q2*q3 - _2q2*ay;

    // Normalize step magnitude
    norm = Math.sqrt(s0*s0 + s1*s1 + s2*s2 + s3*s3);
    if (norm > 1e-9) {
      s0 /= norm; s1 /= norm; s2 /= norm; s3 /= norm;
    } else {
      s0 = s1 = s2 = s3 = 0;
    }

    // Compute rate of change of quaternion from gyroscope
    double qDot0 = 0.5 * (-q1*gx - q2*gy - q3*gz) - beta * s0;
    double qDot1 = 0.5 * ( q0*gx + q2*gz - q3*gy) - beta * s1;
    double qDot2 = 0.5 * ( q0*gy - q1*gz + q3*gx) - beta * s2;
    double qDot3 = 0.5 * ( q0*gz + q1*gy - q2*gx) - beta * s3;

    // Integrate to yield quaternion
    q0 += qDot0 * dt;
    q1 += qDot1 * dt;
    q2 += qDot2 * dt;
    q3 += qDot3 * dt;

    // Normalize quaternion
    norm = Math.sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3);
    if (norm > 0) {
      q0 /= norm; q1 /= norm; q2 /= norm; q3 /= norm;
    } else {
      q0 = 1; q1 = q2 = q3 = 0;
    }
  }

  private void integrateGyro(float gx, float gy, float gz, float dt) {
    double qDot0 = 0.5 * (-q1*gx - q2*gy - q3*gz);
    double qDot1 = 0.5 * ( q0*gx + q2*gz - q3*gy);
    double qDot2 = 0.5 * ( q0*gy - q1*gz + q3*gx);
    double qDot3 = 0.5 * ( q0*gz + q1*gy - q2*gx);

    q0 += qDot0 * dt;
    q1 += qDot1 * dt;
    q2 += qDot2 * dt;
    q3 += qDot3 * dt;

    double norm = Math.sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3);
    if (norm > 0) {
      q0 /= norm; q1 /= norm; q2 /= norm; q3 /= norm;
    } else {
      q0 = 1; q1 = q2 = q3 = 0;
    }
  }
}
