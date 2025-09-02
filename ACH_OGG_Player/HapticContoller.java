package com.example.achoggmusicplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;

/**
 * Haptic 동기화 컨트롤러.
 *
 * - extractPatternOrFallback(): OGG 파일에서 Vorbis Comment 등의 메타로부터
 *   "HAPTIC_MS=..." 같은 키를 읽어 파형을 구성하는 훅.
 *   (지금은 실제 파싱은 TODO, 메타 없으면 폴백 파형을 반환)
 *
 * - playAtOffset(): 전체 파형 기준 특정 오프셋(ms)에서 시작하도록
 *   배열을 재구성하여 createWaveform()으로 재생.
 *
 * - pause(): 현재 진동 중지.
 * - stop(): 완전 종료(동일).
 *
 * 파형 형식:
 *   long[] pattern = {off0, on0, off1, on1, off2, on2, ...}
 *   (첫 원소는 대기시간(off), 이후 off/on 교차. -1 반복 없음)
 */
public final class HapticController {

    private HapticController() {}

    // 현재 재생 중 파형 캐시(필요 시 유지)
    private static long[] lastPattern = null;

    @Nullable
    public static long[] extractPatternOrFallback(Context ctx, Uri mediaUri) {
        // TODO: mediaUri의 OGG Vorbis Comment에서 "HAPTIC_MS=..."를 읽어
        // "0,80,40,120,..." 형태의 문자열을 long[]으로 변환하는 로직을 구현.
        // 당장은 폴백(간단 진동) 사용.
        return new long[]{0, 80, 40, 120, 60, 80, 40, 200};
    }

    public static void playAtOffset(Context ctx, long[] fullPattern, long offsetMs) {
        if (fullPattern == null || fullPattern.length == 0) return;
        lastPattern = fullPattern;

        long[] adjusted = applyOffset(fullPattern, offsetMs);
        vibrate(ctx, adjusted);
    }

    public static void pause(Context ctx) {
        cancel(ctx);
    }

    public static void stop(Context ctx) {
        cancel(ctx);
        lastPattern = null;
    }

    // --- 내부 유틸 ---

    private static void vibrate(Context ctx, long[] timings) {
        if (timings == null || timings.length == 0) return;
        if (Build.VERSION.SDK_INT >= 26) {
            VibrationEffect effect = VibrationEffect.createWaveform(timings, -1);
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vm.getDefaultVibrator().vibrate(effect);
            } else {
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(effect);
            }
        }
    }

    private static void cancel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vm.getDefaultVibrator().cancel();
        } else {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        }
    }

    /**
     * 전체 파형에서 offsetMs만큼 건너뛰어 그 시점부터 재생 가능한 새 파형을 만든다.
     * 예) full = [off0, on0, off1, on1, ...], offset이 on0의 중간이면
     *     남은 on0 시간부터 시작하도록 조정.
     */
    private static long[] applyOffset(long[] full, long offsetMs) {
        if (offsetMs <= 0) return full.clone();

        long acc = 0;
        for (int i = 0; i < full.length; i++) {
            long seg = full[i];
            if (offsetMs < acc + seg) {
                // 이 세그먼트 i의 중간에서 시작
                long remain = (acc + seg) - offsetMs;

                // i 세그먼트의 남은 시간부터 이어서, 뒤의 세그먼트는 그대로 붙인다.
                int restLen = full.length - i;
                long[] adjusted = new long[restLen];
                adjusted[0] = Math.max(0, remain);
                System.arraycopy(full, i + 1, adjusted, 1, restLen - 1);
                return adjusted;
            }
            acc += seg;
        }
        // offset이 전체 길이를 넘으면 더 이상 진동 없음
        return new long[]{0};
    }
}
