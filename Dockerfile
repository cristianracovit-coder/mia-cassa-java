# Usa Eclipse Temurin, il nuovo standard ufficiale e leggero per Java 17
FROM eclipse-temurin:17-jdk-alpine

# Crea una cartella di lavoro dentro il server cloud
WORKDIR /app

# Copia tutti i nostri file (Java, HTML) dal computer al server
COPY . /app/

# Assicura che il file db.txt esista, altrimenti lo crea vuoto
RUN touch db.txt

# Compila il file Java (crea i file .class)
RUN javac main.java

# Dice al Cloud che il nostro server comunica sulla porta 8080
EXPOSE 8080

# Il comando finale per accendere il programma!
CMD ["java", "main"]