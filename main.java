import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class main {
    public static void main(String[] args) throws Exception {
        // Avvia il server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // 1. Mostra la pagina web
        server.createContext("/", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                File htmlFile = new File("index.html");
                if (htmlFile.exists()) {
                    byte[] response = Files.readAllBytes(htmlFile.toPath());
                    t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    t.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate"); // Blocca la cache
                    t.sendResponseHeaders(200, response.length);
                    OutputStream os = t.getResponseBody();
                    os.write(response);
                    os.close();
                } else {
                    String error = "Errore: File index.html non trovato!";
                    t.sendResponseHeaders(404, error.length());
                    t.getResponseBody().write(error.getBytes());
                    t.close();
                }
            }
        });

        // 2. Legge tutto il db.txt 
        server.createContext("/leggi", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                File db = new File("db.txt");
                String response = "";
                if (db.exists()) {
                    // Legge forzando UTF-8
                    response = new String(Files.readAllBytes(db.toPath()), StandardCharsets.UTF_8);
                }
                
                t.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                t.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate"); // FONDAMENTALE PER I DATI AGGIORNATI
                
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(200, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });

        // 3. Riceve il dato, controlla i doppioni e salva in db.txt
        server.createContext("/scrivi", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                if ("POST".equals(t.getRequestMethod())) {
                    try {
                        InputStream is = t.getRequestBody();
                        String input = new String(is.readAllBytes(), StandardCharsets.UTF_8); 
                        
                        String[] parts = input.split(":");
                        if (parts.length == 2) {
                            String nomeRicevuto = parts[0].trim();
                            String importoRicevuto = parts[1].trim();
                            
                            File db = new File("db.txt");
                            if (!db.exists()) db.createNewFile();
                            
                            // Legge i vecchi dati forzando UTF-8
                            List<String> lines = Files.readAllLines(db.toPath(), StandardCharsets.UTF_8);
                            Map<String, String> datiPersonali = new LinkedHashMap<>();
                            
                            for (String riga : lines) {
                                if (riga.contains(":")) {
                                    String[] p = riga.split(":");
                                    datiPersonali.put(p[0].trim(), p[1].trim());
                                }
                            }
                            
                            // Aggiunge o sovrascrive
                            datiPersonali.put(nomeRicevuto, importoRicevuto);
                            
                            // Riscrive l'intero file aggiornato forzando UTF-8
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(db), StandardCharsets.UTF_8));
                            for (Map.Entry<String, String> entry : datiPersonali.entrySet()) {
                                bw.write(entry.getKey() + ":" + entry.getValue());
                                bw.newLine();
                            }
                            bw.close();
                        }
                        
                        String responseMsg = "Dati aggiornati!";
                        byte[] bytes = responseMsg.getBytes(StandardCharsets.UTF_8);
                        t.sendResponseHeaders(200, bytes.length);
                        OutputStream os = t.getResponseBody();
                        os.write(bytes);
                        os.close();
                        
                    } catch (Exception e) {
                        e.printStackTrace(); // Se c'è un problema, ora Java te lo stampa nel terminale!
                        String error = "Errore interno al server";
                        t.sendResponseHeaders(500, error.getBytes(StandardCharsets.UTF_8).length);
                        OutputStream os = t.getResponseBody();
                        os.write(error.getBytes(StandardCharsets.UTF_8));
                        os.close();
                    }
                }
            }
        });

        // 4. Svuota il contenuto del file db.txt
        server.createContext("/svuota", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                if ("POST".equals(t.getRequestMethod())) {
                    try {
                        // Sovrascrive il file scrivendoci il nulla ("") con formato sicuro
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("db.txt"), StandardCharsets.UTF_8));
                        writer.print(""); 
                        writer.close();
                        
                        String responseMsg = "Cassa azzerata con successo!";
                        byte[] bytes = responseMsg.getBytes(StandardCharsets.UTF_8);
                        t.sendResponseHeaders(200, bytes.length);
                        OutputStream os = t.getResponseBody();
                        os.write(bytes);
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        String error = "Errore durante lo svuotamento";
                        t.sendResponseHeaders(500, error.getBytes(StandardCharsets.UTF_8).length);
                        OutputStream os = t.getResponseBody();
                        os.write(error.getBytes(StandardCharsets.UTF_8));
                        os.close();
                    }
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server avviato! Vai su http://localhost:8080");
    }
}