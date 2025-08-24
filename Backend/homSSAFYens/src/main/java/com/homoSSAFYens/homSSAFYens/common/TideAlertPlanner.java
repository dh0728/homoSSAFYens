package com.homoSSAFYens.homSSAFYens.common;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class TideAlertPlanner {

    private TideAlertPlanner() {}

    public record Stats(double vmax, double vmeanPos) {}

    /** +속도만 집계: vmax, vmean(+) */
    public static Stats statsBD(List<BigDecimal> speeds) {
        double vmax = 0, sum = 0; int c=0;
        if (speeds != null) {
            for (BigDecimal b : speeds) {
                if (b == null) continue;
                double v = b.doubleValue();
                if (v > 0) { vmax = Math.max(vmax, v); sum += v; c++; }
            }
        }
        return new Stats(vmax, c>0 ? sum/c : 0);
    }

    /**
     * 상승량 적분(cm) — 속도 단위를 cm/h 가정.
     * 샘플 간격(h) ~ (to-from)/N/3600 로 근사 (최소 1분 보정)
     */
    public static double integrateRiseCm(List<BigDecimal> speeds, long fromEpoch, long toEpoch) {
        if (speeds == null || speeds.isEmpty()) return 0;
        double hoursPerSample = Math.max(1.0/60, (toEpoch - fromEpoch) / 3600.0 / Math.max(1, speeds.size()));
        double s=0;
        for (BigDecimal b : speeds) {
            double v = (b==null?0:b.doubleValue());
            if (v > 0) s += v * hoursPerSample;
        }
        return s;
    }

    /** 속도 기반 동적 오프셋. 앞당김만 적용(뒤로 미루지 않음) */
    public static List<Integer> dynamicOffsets(double vmax, double vmean, double ratio) {
        List<Integer> base;
        if (vmax >= 60)      base = List.of(-120,-90,-60,-30,-15,-5);
        else if (vmax >= 30) base = List.of(-90,-60,-30,-15,-5);
        else if (vmax >= 10) base = List.of(-60,-30,-15,-5);
        else if (vmax > 0)   base = List.of(-30,-15,-5);
        else                 base = List.of(-15);

        int shiftByMean  = Math.min(20, Math.max(0, (int)Math.round(vmean/10.0) * 5));
        int shiftByRatio = Math.min(10, Math.max(0, (int)Math.round((ratio-1.0)*10))); // ratio>1만 반영
        int shift = Math.min(30, shiftByMean + shiftByRatio);

        return base.stream().map(m -> m - shift).distinct().sorted().toList();
    }

    public static List<Integer> adjustOffsets(double vmax, double vmean, double riseEstimateCm, int deltaCm) {
        int off1 = -30;
        int off2 = -10;

        // 기본: shift 없음
        int shift = 0;
        int shift2 = 0;

        if (deltaCm > 0 && riseEstimateCm > deltaCm) {
            // DB 예측이 더 클 때만 shift 계산
            if (vmax >= 60) shift += 15;
            else if (vmax >= 30) shift += 10;
            else if (vmax >= 10) shift += 5;

            // 얼마나 더 큰지 비율로 추가 보정
            double ratio = riseEstimateCm / deltaCm;
            if (ratio > 1.2) shift += 5;
            if (ratio > 1.5) shift += 10;

            shift = Math.min(25, shift);         // -30은 최대 -55
            shift2 = Math.min(15, shift / 2);    // -10은 절반만, 최대 -25
        }

        int adj1 = off1 - shift;
        int adj2 = off2 - shift2;

        log.info("[OffsetCalc] riseEstimateCm={}, deltaCm={}, shift={}, shift2={}, result=[{},{}]",
                String.format("%.2f", riseEstimateCm), deltaCm, shift, shift2, adj1, adj2);

        return List.of(adj1, adj2);
    }
}
