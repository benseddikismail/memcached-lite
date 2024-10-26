import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client2 {
    private static final String FILE_NAME = "data/response_times_file.txt";
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(); 

    public static void main(String[] args) {
        final String host = "localhost";
        final int port = 9090;

        int numClients = 3; // Number of concurrent clients
        int requestsPerClient = 3; // Number of requests per client
        int requestDelay = 1000; // Delay between requests in milliseconds


        new Thread(() -> {
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(FILE_NAME, false)))) {
                writer.println("ClientID,Command,Key,ResponseTime(ms)"); 
                while (true) {
                    String record = queue.take();
                    if (record.equals("EOF")) { 
                        break;
                    }
                    writer.println(record);
                    writer.flush(); 
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            int clientId = i;
            Thread thread = new Thread(() -> {
                Client client = new Client(host, port);
                for (int j = 0; j < requestsPerClient; j++) {
                    String key = "key" + (clientId * requestsPerClient + j);
                    String value = "value" + (clientId * requestsPerClient + j);

                    try {
  
                        long startTime = System.currentTimeMillis();
                        client.setCommand("set", key, value, value.getBytes().length);
                        long endTime = System.currentTimeMillis();
                        long responseTime = endTime - startTime;

                        System.out.println("SET E2E latency: " + responseTime + " ms");

                        queue.put(clientId + ",set," + key + "," + responseTime);

                        Thread.sleep(requestDelay);

                        startTime = System.currentTimeMillis();
                        client.setCommand("get", key, null, 0);
                        endTime = System.currentTimeMillis();
                        responseTime = endTime - startTime;

                        System.out.println("GET E2E latency: " + responseTime + " ms");

                        queue.put(clientId + ",get," + key + "," + responseTime); 

                        Thread.sleep(requestDelay);
                    } catch (Exception e) {
                        System.err.println("Error in client " + clientId + " for key " + key + ": " + e.getMessage());
                    }
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            queue.put("EOF");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
