# Usa un'immagine base di OpenJDK
FROM openjdk:17-jdk-alpine

# Variabile di ambiente per il nome dell'app
ENV APP_NAME=favService

# Copia il jar compilato nel container
COPY target/favService-0.0.1-SNAPSHOT.jar app.jar

# Espone la porta del server Spring Boot
EXPOSE 8084

# Comando per avviare l'app
ENTRYPOINT ["java","-jar","/app.jar"]
