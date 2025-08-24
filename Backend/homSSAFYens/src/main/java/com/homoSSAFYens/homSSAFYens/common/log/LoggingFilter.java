package com.homoSSAFYens.homSSAFYens.common.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    // 바디 최대 로그 길이(바이트)
    private static final int MAX_LOG_PAYLOAD = 2_000; // 2KB

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        // Correlation ID (요청-응답 연동 키)
        String cid = request.getHeader("X-Request-Id");
        if (cid == null || cid.isBlank()) cid = UUID.randomUUID().toString();
        MDC.put("cid", cid);

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(req, res);
        } finally {
            long tookMs = System.currentTimeMillis() - start;

            // 요청 바디
            String reqBody = "";
            if (isReadableBody(req.getContentType(), req.getMethod())) {
                byte[] buf = req.getContentAsByteArray();
                reqBody = abbreviate(new String(buf, 0, Math.min(buf.length, MAX_LOG_PAYLOAD), StandardCharsets.UTF_8));
            }

            // 응답 바디
            String resBody = "";
            String contentType = res.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                byte[] buf = res.getContentAsByteArray();
                resBody = abbreviate(new String(buf, 0, Math.min(buf.length, MAX_LOG_PAYLOAD), StandardCharsets.UTF_8));
            }

            // 민감정보 마스킹 (예: "password": "..." → "******")
            reqBody = maskSensitive(reqBody);
            resBody = maskSensitive(resBody);

            // 헤더 일부만 (전부 찍으면 개인정보/토큰 노출 위험)
            String ua = safe(request.getHeader("User-Agent"));
            String ip = safe(getClientIp(request));

            // 로그 한 번에
            log.info("{} {} ip={} ua={} status={} took={}ms\nreq={}\nres={}",
                    request.getMethod(),
                    request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                    ip, abbreviate(ua),
                    res.getStatus(), tookMs,
                    reqBody, resBody
            );

            // response body 다시 복구하여 클라이언트로 전달
            res.copyBodyToResponse();
            MDC.clear();
        }
    }

    private static boolean isReadableBody(String contentType, String method) {
        if (contentType == null) return false;
        if (!method.equalsIgnoreCase("POST") &&
                !method.equalsIgnoreCase("PUT") &&
                !method.equalsIgnoreCase("PATCH")) return false;
        return contentType.contains("json") || contentType.contains("xml") || contentType.contains("form");
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 1000 ? s.substring(0, 1000) + "...(trunc)" : s;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String getClientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static String maskSensitive(String body) {
        if (body == null) return "";
        // 예시: password, token, authorization 등 마스킹
        body = body.replaceAll("(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
        body = body.replaceAll("(\"access_token\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
        return body;
    }
}
