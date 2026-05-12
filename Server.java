import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int TCP_PORT = 5000;
    private static final int UDP_PORT = 5001;
    private static final int HTTP_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("--- POS Multi-Protocol Server Starting ---");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 1. TCP Server: For persistent, reliable connections (e.g., Syncing)
        executor.execute(() -> startTCPServer());

        // 2. UDP Server: For discovery or heartbeat signals
        executor.execute(() -> startUDPServer());

        // 3. HTTP Server: For web-based monitoring/API
        executor.execute(() -> startHTTPServer());
    }

    private static void startTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("[TCP] Listening on port " + TCP_PORT);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    
                    System.out.println("[TCP] Client connected: " + clientSocket.getInetAddress());
                    String inputLine = in.readLine();
                    if ("SYNC".equals(inputLine)) {
                        out.println("ACK|SYNC_SUCCESSFUL|" + System.currentTimeMillis());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error: " + e.getMessage());
        }
    }

    private static void startUDPServer() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            System.out.println("[UDP] Listening on port " + UDP_PORT);
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[UDP] Received: " + received);
                
                // Echo response
                byte[] response = "POS_SERVER_ALIVE".getBytes();
                DatagramPacket reply = new DatagramPacket(response, response.length, 
                                       packet.getAddress(), packet.getPort());
                socket.send(reply);
            }
        } catch (IOException e) {
            System.err.println("[UDP] Error: " + e.getMessage());
        }
    }

    private static void startHTTPServer() {
    try (ServerSocket httpSocket = new ServerSocket(HTTP_PORT)) {
        System.out.println("[HTTP] Web Dashboard at http://localhost:" + HTTP_PORT);
        while (true) {
            try (Socket client = httpSocket.accept();
                 OutputStream os = client.getOutputStream();
                 PrintWriter out = new PrintWriter(os)) {
                
                // Read product data from the file
                StringBuilder tableRows = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader("products.txt"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] d = line.split("\\|");
                        tableRows.append(String.format(
                            "<tr><td>%s</td><td>%s</td><td>%s</td><td>$%s</td><td>%s</td></tr>",
                            d[0], d[1], d[2], d[3], d[4]
                        ));
                    }
                } catch (IOException e) {
                    tableRows.append("<tr><td colspan='5'>No data found. Start POS first!</td></tr>");
                }

                // Send HTTP Headers
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println("\r\n");
                
                // Send HTML Content
                out.println("<html><head><style>table{width:100%; border-collapse:collapse;} th,td{border:1px solid #ddd; padding:8px; text-align:left;} th{background-color: #4CAF50; color:white;}</style></head>");
                out.println("<body><h1>POS Live Inventory Dashboard</h1>");
                out.println("<table><tr><th>Type</th><th>ID</th><th>Name</th><th>Price</th><th>Stock</th></tr>");
                out.println(tableRows.toString());
                out.println("</table>");
                out.println("<p>Last Updated: " + new java.util.Date() + "</p>");
                out.println("<script>setTimeout(() => { location.reload(); }, 5000);</script>"); // Auto-refresh every 5s
                out.println("</body></html>");
                out.flush();
            }
        }
    } catch (IOException e) {
        System.err.println("[HTTP] Error: " + e.getMessage());
    }
}
}