import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class ClientTest {

    private static final int PORT = 9090;
    private static Thread serverThread;
    private Client client;
    private static final Path dirPath = Paths.get("src/dir/");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        cleanDirectory(dirPath);
        serverThread = new Thread(() -> {
            try {
                Server.main(new String[]{}); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(1000);
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        Client shutdownClient = new Client("localhost", 9090);
        shutdownClient.setCommand("shutdown", null, null, 0);
        serverThread.join();
    }

    @BeforeEach
    public void setUp() {
        client = new Client("localhost", PORT);
    }

    @Test
    public void testSetCommand() {
        String key = "testKey-1";
        String value = "This is testValue-1";
        client.setCommand("set", key, value, value.getBytes().length);
        Path filePath = Paths.get("src/dir/", key + ".txt");
        assertTrue(Files.exists(filePath), "File should exist after set command.");
        try {
            List<String> fileContent = Files.readAllLines(filePath);
            assertEquals(String.valueOf(0), fileContent.get(0).trim(), "First line should contain the expiration time.");
            assertEquals(String.valueOf(0), fileContent.get(1).trim(), "Second line should contain the flags.");
            assertEquals(value, fileContent.get(2).trim(), "Third line should match the set value.");
        } catch (IOException e) {
            fail("Failed to read file content: " + e.getMessage());
        }
    }

    @Test
    public void testGetCommand() {
        String key = "testKey-2";
        String value = "This is testValue-2";
        client.setCommand("set", key, value, value.getBytes().length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        client.setCommand("get", key, null, 0);
        System.setOut(originalOut);
        String output = outputStream.toString();
        assertTrue(output.contains("Key: " + key), "Output should contain the retrieved key.");
        assertTrue(output.contains("Value: " + value), "Output should contain the retrieved value.");
    }

    private static void cleanDirectory(Path path) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    Files.delete(entry);
                }
            }
        }
    }

}
