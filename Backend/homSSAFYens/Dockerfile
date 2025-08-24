FROM azul/zulu-openjdk:17.0.14-17.56

WORKDIR /app

# 필요한 패키지 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드된 jar 파일 복사
ARG JAR_FILE
COPY ${JAR_FILE} app.jar

# 포트 노출
EXPOSE 8552

# 작업 디렉토리 권한 설정
RUN chown -R 1001:1001 /app

# 비루트 사용자로 전환
USER 1001

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -k -f http://localhost:8552/actuator/health || exit 1

# JVM 최적화 옵션 포함하여 애플리케이션 실행
CMD ["java", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+UnlockExperimentalVMOptions", \
     "-XX:+UseContainerSupport", \
     "-Xms1g", \
     "-Xmx2g", \
     "-jar", "app.jar"]
