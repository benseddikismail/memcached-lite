import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class Server {

    private static Random random = new Random();

    public static void main(String[] args) {

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

                try (Socket clientSocket = serverSocket.accept()) { /* close client socket automatically */
                    System.out.println("Client connected!");

                    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
    
                        String request;
                        while ((request = in.readLine()) != null) {
                            if (request.trim().isEmpty()) continue; 

                            if (!validCommands.stream().anyMatch(request::startsWith)){
                                out.print("ERROR\r\n"); /* unknown command */
                                out.flush();
                                continue;
                            }

                            if (request.startsWith("set ")) {
                                try {
                                    long startTime = System.currentTimeMillis();
                                    long endTime;
                                    String[] request_parts = request.split("\r\n");
                                    String header = request_parts[0];
                                    String [] header_parts = header.split(" ");
                                    String key = header_parts[1];
                                    int flags = header_parts.length >= 5 ? Integer.parseInt(header_parts[2]) : 0; 
                                    int expiration_time = header_parts.length >= 5 ? Integer.parseInt(header_parts[3]) : 0;
                                    int bytes = header_parts.length >= 5  ? Integer.parseInt(header_parts[4]) : Integer.parseInt(header_parts[2]);
                                    boolean no_reply = header_parts.length > 5 && "noreply".equalsIgnoreCase(header_parts[5]);

                                    String value = in.readLine();
                                    if (value.getBytes().length != bytes) {
                                        out.print("CLIENT_ERROR bad data chunk\r\n");
                                        out.flush();
                                        continue;
                                    }
                                    if (setValue(key, value, flags, expiration_time)) {
                                        if (!no_reply)
                                            out.println("STORED\r\n");
                                        endTime = System.currentTimeMillis();
                                    } else {
                                        if (!no_reply)
                                            out.println("NOT-STORED\r\n");
                                        endTime = System.currentTimeMillis();
                                    }
                                    System.out.println("Time taken to process SET request: " + (endTime - startTime) + " ms");
                                } catch (Exception e) {
                                    out.print("SERVER_ERROR " + e.getMessage() + "\r\n");
                                    out.flush();
                                }
                            } else if (request.startsWith("get ")) {
                                long startTime = System.currentTimeMillis();
                                String key = request.substring(4).trim();
                                getValue(key, out);
                                long endTime = System.currentTimeMillis();
                                System.out.println("Time taken to process GET request: " + (endTime - startTime) + " ms");
                            } else if (request.startsWith("delete ")) {
                                String[] parts = request.split(" ");
                                String key = parts[1];
                                boolean noreply = parts.length > 2 && "noreply".equals(parts[2]);
                                if (deleteValue(key) && !noreply) {
                                    out.println("DELETED\r\n");
                                } else if (!noreply) {
                                    out.println("NOT_FOUND\r\n");
                                }
                            } else if (request.equalsIgnoreCase("shutdown")) {  /* for testing purposes */
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

        } catch (IOException e) {
            System.out.println("Error starting server on port " + port + ": " + e.getMessage());
        }
    }

    private static void simulateDelay() {
        int delay = random.nextInt(1000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean setValue(String key, String value, int flags, int expiration_time) {
        String filename = key + ".txt";
        File dir = new File("src/dir/");
        simulateDelay();
        long expiration;
        if (expiration_time > 0) {
            expiration = System.currentTimeMillis() + (expiration_time * 1000L);
        } else {
            expiration = 0;
        }
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir, filename);
        try {
            if (file.createNewFile()) {
                try (FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.write(expiration + "\n");
                    fileWriter.write(flags + "\n");
                    fileWriter.write(value);
                    return true; 
                }
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
        return false;
    }

    private static void getValue(String key, PrintWriter out) {
        String filename = key + ".txt";
        File dir = new File("src/dir/");
        simulateDelay();
        StringBuilder value = new StringBuilder();
        File file = new File(dir, filename);
        if (file.exists() && file.isFile()) {
            try {
                Scanner reader = new Scanner(file);
                long expiration_time = Long.parseLong(reader.nextLine());
                if (expiration_time > 0 && System.currentTimeMillis() > expiration_time) {
                    out.print("END\r\n");
                    out.flush();
                    return;
                }
                int flags = Integer.parseInt(reader.nextLine());
                while (reader.hasNextLine()) {
                    String line = reader.nextLine();
                    value.append(line).append(System.lineSeparator());
                }
                reader.close();
                String datablock = value.toString();
                int bytes = datablock.getBytes().length;
                out.printf("VALUE %s %d %d\r\n", key, flags, bytes);
                out.flush();
                out.print(datablock + "\r\n");
                out.flush();  
                out.print("END\r\n");
                out.flush();  
            } catch (FileNotFoundException e) {
                out.print("SERVER_ERROR " + e.getMessage() + "\r\n");
                out.flush();
                System.out.println("Error reading file: " + e.getMessage());
            } 
        }
    }

    private static boolean deleteValue(String key) {
        String filename = key + ".txt";
        File dir = new File("src/dir/");
        simulateDelay();
        File file = new File(dir, filename);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                System.out.println("Failed to delete file.");
            }
        }
        return false;
    }

}


