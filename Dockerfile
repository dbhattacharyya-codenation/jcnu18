FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY . /app

RUN ./gradlew build

CMD ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","build/libs/firstapp-1.0-SNAPSHOT.jar"]
