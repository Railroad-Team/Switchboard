FROM openjdk:24
WORKDIR /opt/Switchboard/
COPY build/libs/Switchboard-all.jar Switchboard.jar
CMD ["java", "-jar", "Switchboard.jar"]