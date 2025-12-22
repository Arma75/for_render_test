# 1. 빌드 단계 (Build Stage)
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
# gradlew에 실행 권한을 주고 빌드합니다.
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 2. 실행 단계 (Run Stage)
# 에러가 났던 openjdk 대신, 동일한 환경인 amazoncorretto를 사용합니다.
FROM amazoncorretto:17-alpine
WORKDIR /app
# 빌드된 결과물 폴더에서 jar 파일을 복사합니다.
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]