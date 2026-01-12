# ===== 1. Build Stage =====
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# 1-1. Gradle 빌드 환경 복사 (캐시 활용을 위해 소스보다 먼저 복사)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 1-2. gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 1-3. 의존성 다운로드 (소스코드 변경 시에도 이 레이어는 캐시됨)
RUN ./gradlew dependencies --no-daemon

# 1-4. 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew build -x test --no-daemon

# ===== 2. Run Stage =====
FROM eclipse-temurin:17-jre

WORKDIR /app

# 2-1. 보안: root가 아닌 별도 계정 생성 및 사용
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

# 2-2. 타임존 설정 (한국 시간) - 필요시 사용
ENV TZ=Asia/Seoul

# 2-3. 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# 2-4. 실행 (보안 및 신호 처리를 위해 exec 형태 유지)
ENTRYPOINT ["java", "-jar", "app.jar"]
