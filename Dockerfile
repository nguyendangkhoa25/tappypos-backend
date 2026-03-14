FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="nguyendangkhoa25@gmail.com"

ENV TZ="Asia/Ho_Chi_Minh"
RUN mkdir -p /app/logs

COPY target/retail-platform-1.0.0.jar /app/retail-platform.jar

EXPOSE 6868

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/retail-platform.jar"]
