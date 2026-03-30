import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
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

        // 2. Legge tutto il db.txt (ci serve per calcolare la media nell'HTML)
        server.createContext("/leggi", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                File db = new File("db.txt");
                String response = "";
                if (db.exists()) {
                    response = new String(Files.readAllBytes(db.toPath()));
                }
                t.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                t.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        // 3. Riceve il dato, controlla i doppioni e salva in db.txt
        server.createContext("/scrivi", new HttpHandler() {
            public void handle(HttpExchange t) throws IOException {
                if ("POST".equals(t.getRequestMethod())) {
                    InputStream is = t.getRequestBody();
                    String input = new String(is.readAllBytes()); // Riceve es: "Marco:10.50"
                    
                    String[] parts = input.split(":");
                    if (parts.length == 2) {
                        String nomeRicevuto = parts[0].trim();
                        String importoRicevuto = parts[1].trim();
                        
                        File db = new File("db.txt");
                        if (!db.exists()) db.createNewFile();
                        
                        // Legge i vecchi dati e li mette in una Mappa (Dizionario)
                        List<String> lines = Files.readAllLines(db.toPath());
                        Map<String, String> datiPersonali = new LinkedHashMap<>();
                        
                        for (String riga : lines) {
                            if (riga.contains(":")) {
                                String[] p = riga.split(":");
                                datiPersonali.put(p[0].trim(), p[1].trim());
                            }
                        }
                        
                        // AGGIUNGE O SOVRASCRIVE: se il nome esiste, cambia solo l'importo!
                        datiPersonali.put(nomeRicevuto, importoRicevuto);
                        
                        // Riscrive l'intero file aggiornato
                        BufferedWriter bw = new BufferedWriter(new FileWriter(db));
                        for (Map.Entry<String, String> entry : datiPersonali.entrySet()) {
                            bw.write(entry.getKey() + ":" + entry.getValue());
                            bw.newLine();
                        }
                        bw.close();
                    }
                    
                    String response = "Dati aggiornati!";
                    t.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server avviato! Vai su http://localhost:8080");
    }
}