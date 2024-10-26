import java.io.*;
import java.net.*;


public class Client {
        
    private final String host;
    private final int port;
    private String key;
    private String value; 
    private String command;
    private int bytes;

    public Client(String host, int port) {
        this.host = host;   
        this.port = port;
    }

    public void setCommand(String command, String key, String value, int bytes) {
        this.command = command;
        this.key = key;
        this.value = value;
        this.bytes = (bytes > 0 || (bytes == 0 && value == null)) ? bytes : value.getBytes().length;
        this.send(); 
    }

    private void send() {

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            if (command.equals("set")) {
                final String set_command = String.format("set %s %d\r\n%s\r\n", key, bytes, value);
                System.out.println("Sending set request to server...");
                long startTime = System.currentTimeMillis();
                out.print(set_command);
                out.flush();
                String response = in.readLine();
                long endTime = System.currentTimeMillis();
                // System.out.println("Time taken to receive SET response: " + (endTime - startTime) + "ms");
                if (response != null && (response.startsWith("STORED") || response.startsWith("NOT-STORED"))) {
                    System.out.println("Response from server: " + response);
                } else {
                    System.out.println("Unexpected response: " + response);
                }
            } 
            else if (command.equals("get")) {    
                final String get_command = String.format("get %s\r\n", key);
                System.out.println("Sending get request to server...");
                out.print(get_command);
                out.flush();

                System.out.println("Response from server:");
                String response = in.readLine();
                if (response != null && response.startsWith("VALUE")) {
                    String [] response_parts = response.split(" ");
                    String response_key = response_parts[1];
                    int flags = Integer.parseInt(response_parts[2]);
                    int bytes = Integer.parseInt(response_parts[3]);
                    char[] response_value = new char[bytes];
                    int read_bytes = in.read(response_value, 0, bytes);
                    if (read_bytes == bytes) {
                        in.readLine();
                        String response_end = in.readLine();
                        if (response_end.equals("END")) {
                            System.out.println("Key: " + response_key);
                            System.out.println("Value: " + new String(response_value));
                        } else {
                            System.out.println("Missing END token in response");
                        }
                    } else {
                        System.out.println("Error: Incomplete data block received.");
                    }
                } else {
                    System.out.println("Unexpected response: " + response);
                }
            } 
            else if (command.equals("shutdown")) {  /* for testing purposes */
                System.out.println("Sending shutdown request to server...");
                out.println("shutdown");
                String response = in.readLine();
                System.out.println("Response from server: " + response);
            } else {
                System.out.println("Unknown command: " + command);
                return;
            }
     
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        
        final String host = "localhost";
        final int port = 9090;

        Client client = new Client(host, port);
        client.setCommand("set", "exampleKey-1", "This is an example value", 0);
        client.setCommand("get", "exampleKey-1", null, 0);
        client.setCommand("get", "exampleKey-1", null, 0);

    }
}
