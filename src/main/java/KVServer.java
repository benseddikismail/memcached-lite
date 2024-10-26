import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class KVServer {

    private static Firestore db;
    private static Random random = new Random();

    public static void main(String[] args) throws IOException {

        db = FirestoreOptions.getDefaultInstance().getService();

        final int port = 9090;
        final Set<String> validCommands = new HashSet<>(Arrays.asList(
            "set", "add", "replace", "append", "prepend", "cas",
            "get", "gets",
            "delete",
            "incr", "decr",
            "flush_all", "version", "stats", "quit",
            "shutdown"
        ));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            
            System.out.println("Server is running on port " + port + " and waiting for clients...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected!");

                    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
    
                        String request;
                        while ((request = in.readLine()) != null) {
                            if (request.trim().isEmpty()) continue;

                            if (!validCommands.stream().anyMatch(request::startsWith)){
                                out.print("ERROR\r\n");
                                out.flush();
                                continue;
                            }

                            if (request.startsWith("set ")) {
                                handleSetCommand(request, in, out);
                            } else if (request.startsWith("get ")) {
                                handleGetCommand(request, out);
                            } else if (request.startsWith("delete ")) {
                                handleDeleteCommand(request, out);
                            } else if (request.equalsIgnoreCase("shutdown")) {
                                System.out.println("Shutdown command received. Stopping server...");
                                out.println("Server shutting down...\r\n");
                                return;
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error handling client I/O: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.out.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    private static void handleSetCommand(String request, BufferedReader in, PrintWriter out) throws IOException {
        try {
            long startTime = System.currentTimeMillis();
            String[] requestParts = request.split("\r\n");
            String header = requestParts[0];
            String[] headerParts = header.split(" ");
            String key = headerParts[1];
            int flags = headerParts.length >= 5 ? Integer.parseInt(headerParts[2]) : 0;
            int expirationTime = headerParts.length >= 5 ? Integer.parseInt(headerParts[3]) : 0;
            int bytes = headerParts.length >= 5 ? Integer.parseInt(headerParts[4]) : Integer.parseInt(headerParts[2]);
            boolean noReply = headerParts.length > 5 && "noreply".equalsIgnoreCase(headerParts[5]);

            String value = in.readLine();
            if (value.getBytes().length != bytes) {
                out.print("CLIENT_ERROR bad data chunk\r\n");
                out.flush();
                return;
            }

            // Store the value in Firestore
            if (setValue(key, value, flags, expirationTime)) {
                if (!noReply)
                    out.println("STORED\r\n");
            } else {
                if (!noReply)
                    out.println("NOT-STORED\r\n");
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Time taken to process SET request: " + (endTime - startTime) + " ms");
        } catch (Exception e) {
            out.print("SERVER_ERROR " + e.getMessage() + "\r\n");
            out.flush();
        }
    }

    private static void handleGetCommand(String request, PrintWriter out) {
        long startTime = System.currentTimeMillis();
        String key = request.substring(4).trim();
        getValue(key, out);
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken to process GET request: " + (endTime - startTime) + " ms");
    }

    private static void handleDeleteCommand(String request, PrintWriter out) {
        String[] parts = request.split(" ");
        String key = parts[1];
        boolean noReply = parts.length > 2 && "noreply".equals(parts[2]);
        if (deleteValue(key) && !noReply) {
            out.println("DELETED\r\n");
        } else if (!noReply) {
            out.println("NOT_FOUND\r\n");
        }
    }

    private static boolean setValue(String key, String value, int flags, int expirationTime) throws ExecutionException, InterruptedException {
        // Firestore document data
        Map<String, Object> data = new HashMap<>();
        data.put("value", value);
        data.put("flags", flags);
        if (expirationTime > 0) {
            data.put("expiration", System.currentTimeMillis() + (expirationTime * 1000L));
        }

        // Set value in Firestore
        ApiFuture<WriteResult> future = db.collection("keyValueStore").document(key).set(data);
        future.get();  // Wait for the operation to complete
        return true;
    }

    private static void getValue(String key, PrintWriter out) {
        try {
            DocumentReference docRef = db.collection("keyValueStore").document(key);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                Map<String, Object> data = document.getData();
                long expiration = (Long) data.getOrDefault("expiration", 0L);
                if (expiration > 0 && System.currentTimeMillis() > expiration) {
                    out.print("END\r\n");
                    out.flush();
                    return;
                }
                String value = (String) data.get("value");
                int flags = ((Long) data.get("flags")).intValue(); // Updated line
                int bytes = value.getBytes().length;
                out.printf("VALUE %s %d %d\r\n", key, flags, bytes);
                out.print(value + "\r\n");
                out.print("END\r\n");
                out.flush();
            } else {
                out.print("NOT_FOUND\r\n");
                out.flush();
            }
        } catch (Exception e) {
            out.print("SERVER_ERROR " + e.getMessage() + "\r\n");
            out.flush();
        }
    }


    private static boolean deleteValue(String key) {
        try {
            ApiFuture<WriteResult> writeResult = db.collection("keyValueStore").document(key).delete();
            writeResult.get();  // Wait for the operation to complete
            return true;
        } catch (Exception e) {
            System.out.println("Error deleting key: " + e.getMessage());
            return false;
        }
    }
}
