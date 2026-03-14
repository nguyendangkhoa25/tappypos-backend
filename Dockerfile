FROM eclipse-temurin:17-jdk-alpine
LABEL maintainer="nguyendangkhoa25@gmail.com"

ENV TZ="Asia/Ho_Chi_Minh"
COPY target/retail-platform-1.0.0.jar retail-platform-0.0.1.jar
EXPOSE 6868
ENTRYPOINT ["java","-jar","/retail-platform-0.0.1.jar"]
