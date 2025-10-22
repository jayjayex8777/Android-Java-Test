package com.example.qtniontest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  private SensorManager sensorManager;
  private Sensor accelSensor;
  private Sensor gyroSensor;

  // Madgwick filter (quaternion-based fusion)
  private MadgwickAHRS ahrs;

  // Latest raw sensor values
  private float[] acc = new float[3];
  private float[] gyr = new float[3];
  private boolean hasAcc = false, hasGyr = false;

  // Timestamp (ns) for dt
  private long lastGyroTimestampNs = 0;

  // Orientation quaternions
  private final double[] qCurr = new double[]{1,0,0,0}; // [w,x,y,z]
  private final double[] qInit = new double[]{1,0,0,0}; // reference (resettable)
  private boolean hasInit = false;

  // Diagonal axis in device coordinates (top-left to bottom-right in screen plane).
  // Android device coordinates (portrait natural orientation): +x right, +y up, +z toward the user.
  // Screen TL -> BR ≈ (+x, -y, 0)
  private final double[] diagAxisBody = norm(new double[]{1, -1, 0});

  // UI
  private TextView tvAngle, tvQuat, tvDt, tvStatus;
  private Button btnReset;

  // For simple FPS / dt display
  private long lastUiUpdateMs = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    tvAngle = findViewById(R.id.tvAngle);
    tvQuat  = findViewById(R.id.tvQuat);
    tvDt    = findViewById(R.id.tvDt);
    tvStatus= findViewById(R.id.tvStatus);
    btnReset= findViewById(R.id.btnReset);

    btnReset.setOnClickListener(v -> resetReference());

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    gyroSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    // Madgwick: beta는 가속도/자이로 노이즈 수준에 맞춰 조절 (기본 0.1~0.2 권장)
    ahrs = new MadgwickAHRS(0.1f);

    if (accelSensor == null || gyroSensor == null) {
      tvStatus.setText("⚠️ 이 기기에서 가속도계 또는 자이로스코프를 사용할 수 없습니다.");
    } else {
      tvStatus.setText("Sensors OK · Madgwick running");
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // SENSOR_DELAY_GAME 정도가 반응성과 부하 밸런스가 좋습니다.
    sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
    sensorManager.registerListener(this, gyroSensor,  SensorManager.SENSOR_DELAY_GAME);
  }

  @Override
  protected void onPause() {
    super.onPause();
    sensorManager.unregisterListener(this);
  }

  @SuppressLint("DefaultLocale")
  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      System.arraycopy(event.values, 0, acc, 0, 3);
      hasAcc = true;
    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
      System.arraycopy(event.values, 0, gyr, 0, 3);
      hasGyr = true;

      final long ts = event.timestamp; // ns
      if (lastGyroTimestampNs != 0L) {
        float dt = (ts - lastGyroTimestampNs) * 1e-9f; // seconds

        if (hasAcc) {
          // Madgwick update: gyro(rad/s), accel(m/s^2)
          ahrs.update(gyr[0], gyr[1], gyr[2], acc[0], acc[1], acc[2], dt);
          double[] q = ahrs.getQuaternion(); // [w,x,y,z]

          // Copy to current
          qCurr[0] = q[0]; qCurr[1] = q[1]; qCurr[2] = q[2]; qCurr[3] = q[3];

          // Initialize reference orientation once (or after reset)
          if (!hasInit) {
            qInit[0] = qCurr[0]; qInit[1] = qCurr[1]; qInit[2] = qCurr[2]; qInit[3] = qCurr[3];
            hasInit = true;
          }

          // Relative rotation q_rel = inv(qInit) * qCurr
          double[] qInitInv = quatConjugate(qInit); // unit quaternion => inverse = conjugate
          double[] qRel = quatMultiply(qInitInv, qCurr);
          qRel = quatNormalize(qRel);

          // Swing-Twist decomposition (twist about diagAxisBody at t0)
          // Twist quaternion = keep only vector part parallel to axis
          double axisDot = qRel[1]*diagAxisBody[0] + qRel[2]*diagAxisBody[1] + qRel[3]*diagAxisBody[2]; // q_vec · axis
          double w = qRel[0];
          // Signed twist angle (radians) around the chosen axis:
          double angleRad = 2.0 * Math.atan2(axisDot, w);
          double angleDeg = Math.toDegrees(angleRad);

          // UI throttling (~30–40 Hz)
          long now = SystemClock.elapsedRealtime();
          if (now - lastUiUpdateMs > 25) {
            lastUiUpdateMs = now;
            tvAngle.setText(String.format("Diagonal-axis angle: %7.2f°", angleDeg));
            tvQuat.setText(String.format("q = [w=%.4f, x=%.4f, y=%.4f, z=%.4f]", qCurr[0], qCurr[1], qCurr[2], qCurr[3]));
            tvDt.setText(String.format("dt=%.3f ms", dt*1000.0));
          }
        }
      }
      lastGyroTimestampNs = ts;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // Not used
  }

  private void resetReference() {
    // Set current orientation as zero (reference)
    double[] q = ahrs.getQuaternion();
    qInit[0] = q[0]; qInit[1] = q[1]; qInit[2] = q[2]; qInit[3] = q[3];
    hasInit = true;
    tvStatus.setText("Reference reset (0°) at current pose");
  }

  // ---------- Math helpers ----------

  private static double[] quatConjugate(double[] q) {
    return new double[]{ q[0], -q[1], -q[2], -q[3] };
  }

  private static double[] quatMultiply(double[] a, double[] b) {
    // (w,x,y,z)
    double w = a[0]*b[0] - a[1]*b[1] - a[2]*b[2] - a[3]*b[3];
    double x = a[0]*b[1] + a[1]*b[0] + a[2]*b[3] - a[3]*b[2];
    double y = a[0]*b[2] - a[1]*b[3] + a[2]*b[0] + a[3]*b[1];
    double z = a[0]*b[3] + a[1]*b[2] - a[2]*b[1] + a[3]*b[0];
    return new double[]{w,x,y,z};
  }

  private static double[] quatNormalize(double[] q) {
    double n = Math.sqrt(q[0]*q[0]+q[1]*q[1]+q[2]*q[2]+q[3]*q[3]);
    if (n == 0) return new double[]{1,0,0,0};
    return new double[]{ q[0]/n, q[1]/n, q[2]/n, q[3]/n };
  }

  private static double[] norm(double[] v) {
    double n = Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
    if (n == 0) return new double[]{0,0,0};
    return new double[]{ v[0]/n, v[1]/n, v[2]/n };
  }
}
