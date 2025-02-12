FROM openjdk:21
ARG JAR_FILE=CosmitologistsOffice/target/*.jar
ENV BOT_NAME=Cosmetologistsoffice_bot.
ENV BOT_TOKEN=
ENV BOT_DB_USERNAME=postgres
ENV BOT_DB_PASSWORD=postgres
COPY ${JAR_FILE} CosmitologistsOffice-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-Dspring.datasource.username=${BOT_DB_USERNAME}", "-Dspring.datasource.password=${BOT_DB_PASSWORD}", "-Dbot.username=${BOT_NAME}", "-Dbot.token=${BOT_TOKEN}", "-jar", "/CosmitologistsOffice-0.0.1-SNAPSHOT.jar"]
