# Usa OpenJDK 17 come base
FROM openjdk:17


# Copia il jar dell'applicazione
COPY target/favService-0.0.1-SNAPSHOT.jar app.jar



# Copia lo script wait-for-it (opzionale se vuoi aspettare il DB)
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh


# Espone la porta 8081
EXPOSE 8081


# Avvia l'app
# ATTENZIONE: 'host.docker.internal' punta al DB sul tuo PC
ENTRYPOINT ["./wait-for-it.sh", "host.docker.internal:5432", "--timeout=30", "--strict", "--", "java", "-jar", "app.jar"]

