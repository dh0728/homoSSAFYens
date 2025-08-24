package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.repo.Keys;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 기기별 알림 오프셋(분)을 가져온다.
 * - device 해시에 offsets가 있으면 그 값을,
 * - 없으면 application.yml의 기본값을 사용.
 * - 일단 지금은 offsets을 받진 않지만 추후에 기능 발전을 위해 미리 만들어 놓기
 */
@Service
@RequiredArgsConstructor
public class OffsetService {

    private final StringRedisTemplate redis;

    @Value("${alarm.defaults.offsets:-30,-10}") // 기본값도 넣어둠
    private String defaultOffsetsRaw;            // <- 문자열로 받기

    private List<Integer> defaults() {
        String raw = defaultOffsetsRaw.replace("[","").replace("]","");
        List<Integer> out = new ArrayList<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(Integer.parseInt(t));
        }
        return out.isEmpty() ? List.of(-30, -10) : out;
    }

    public List<Integer> forDevice(String deviceId) {
        String raw = (String) redis.opsForHash().get(Keys.device(deviceId), "offsets");
        if (raw == null || "null".equals(raw)) return defaults();
        try {
            raw = raw.replace("[","").replace("]","");
            List<Integer> list = new ArrayList<>();
            for (String s : raw.split(",")) {
                if (!s.trim().isEmpty()) list.add(Integer.parseInt(s.trim()));
            }
            return list.isEmpty() ? defaults() : list;
        } catch (Exception e) {
            return defaults();
        }
    }
}
