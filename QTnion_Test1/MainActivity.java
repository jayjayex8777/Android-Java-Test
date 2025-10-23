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

  // --- Gyro bias estimation (stationary-based EMA) ---
  private final float[] gyroBias = new float[]{0f, 0f, 0f};
  private static final float G = 9.80665f;
  // 정지 판정 임계값(경험치): 자이로 노름(rad/s)과 중력 크기 편차(m/s^2)
  private static final float STATIONARY_GYRO_NORM_THRESH = 0.08f;   // ~4.6°/s
  private static final float STATIONARY_ACC_DEV_THRESH   = 0.80f;   // | |a|-g |
  // EMA 계수(정지 시에만 갱신)
  private static final float BIAS_EMA_ALPHA = 0.003f;

  // Snap-to-zero 조건
  private static final double SNAP_ANGLE_DEG_THRESH = 2.0;
  private static final float  SNAP_GYRO_NORM_THRESH = 0.05f;
  private static final float  SNAP_ACC_DEV_THRESH   = 0.60f;

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
  private TextView tvAngle, tvQuat, tvDt, tvStatus, tvGForce; // <-- G-force 추가
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
    tvGForce= findViewById(R.id.tvGForce); // <-- G-force 추가
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
      tvStatus.setText("Sensors OK · Madgwick running · Bias & Snap enabled");
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
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
          // ---------- 1) 정지 감지 기반 자이로 바이어스 추정 ----------
          final float gyroNorm = (float)Math.sqrt(gyr[0]*gyr[0] + gyr[1]*gyr[1] + gyr[2]*gyr[2]);
          final float accMag   = (float)Math.sqrt(acc[0]*acc[0] + acc[1]*acc[1] + acc[2]*acc[2]);
          final float accDev   = Math.abs(accMag - G);
          final boolean isStationaryForBias =
              (gyroNorm < STATIONARY_GYRO_NORM_THRESH) && (accDev < STATIONARY_ACC_DEV_THRESH);

          if (isStationaryForBias) {
            // EMA: bias = (1-α)*bias + α*gyro_meas
            gyroBias[0] = (1f - BIAS_EMA_ALPHA) * gyroBias[0] + BIAS_EMA_ALPHA * gyr[0];
            gyroBias[1] = (1f - BIAS_EMA_ALPHA) * gyroBias[1] + BIAS_EMA_ALPHA * gyr[1];
            gyroBias[2] = (1f - BIAS_EMA_ALPHA) * gyroBias[2] + BIAS_EMA_ALPHA * gyr[2];
          }

          // 바이어스 보정된 자이로
          final float gx = gyr[0] - gyroBias[0];
          final float gy = gyr[1] - gyroBias[1];
          final float gz = gyr[2] - gyroBias[2];

          // ---------- 2) Madgwick 업데이트 ----------
          ahrs.update(gx, gy, gz, acc[0], acc[1], acc[2], dt);
          double[] q = ahrs.getQuaternion(); // [w,x,y,z]
          qCurr[0] = q[0]; qCurr[1] = q[1]; qCurr[2] = q[2]; qCurr[3] = q[3];

          if (!hasInit) {
            qInit[0] = qCurr[0]; qInit[1] = qCurr[1]; qInit[2] = qCurr[2]; qInit[3] = qCurr[3];
            hasInit = true;
          }

          // 상대 회전 q_rel = inv(qInit) * qCurr
          double[] qInitInv = quatConjugate(qInit); // 유니트 쿼터니언 가정
          double[] qRel = quatMultiply(qInitInv, qCurr);
          qRel = quatNormalize(qRel);

          // 대각선 축 기준 twist 각도
          double axisDot = qRel[1]*diagAxisBody[0] + qRel[2]*diagAxisBody[1] + qRel[3]*diagAxisBody[2]; // q_vec · axis
          double w = qRel[0];
          double angleRad = 2.0 * Math.atan2(axisDot, w);
          double angleDeg = Math.toDegrees(angleRad);

          // ---------- 3) Snap-to-zero 적용(표시각만 스냅) ----------
          final boolean canSnap =
              (Math.abs(angleDeg) <= SNAP_ANGLE_DEG_THRESH) &&
              (gyroNorm < SNAP_GYRO_NORM_THRESH) &&
              (accDev   < SNAP_ACC_DEV_THRESH);

          if (canSnap) {
            angleDeg = 0.0;
          }

          // ---------- 4) G-force 계산 및 표시 ----------
          final double gForce = accMag / G; // phone feels |a| in g units

          // UI 업데이트(쓰로틀)
          long now = SystemClock.elapsedRealtime();
          if (now - lastUiUpdateMs > 25) {
            lastUiUpdateMs = now;
            tvAngle.setText(String.format("Diagonal-axis angle: %7.2f°", angleDeg));
            tvQuat.setText(String.format("q = [w=%.4f, x=%.4f, y=%.4f, z=%.4f]", qCurr[0], qCurr[1], qCurr[2], qCurr[3]));
            tvDt.setText(String.format("dt=%.3f ms", dt*1000.0));
            tvStatus.setText(String.format(
                "Bias[rad/s]=[%.4f, %.4f, %.4f] · stationary=%s",
                gyroBias[0], gyroBias[1], gyroBias[2],
                isStationaryForBias ? "Y" : "N"
            ));
            tvGForce.setText(String.format("G-force: %.3f g (|a|=%.3f m/s²)", gForce, accMag));
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
