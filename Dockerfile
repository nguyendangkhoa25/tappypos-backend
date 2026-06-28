FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="nguyendangkhoa25@gmail.com"

ENV TZ="Asia/Ho_Chi_Minh"
WORKDIR /app
RUN mkdir -p /app/logs

COPY target/tappy-shop-1.0.0.jar /app/tappy-shop.jar

EXPOSE 6868

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/tappy-shop.jar"]
