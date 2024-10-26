public class Client1 {
    public static void main(String[] args) {
        final String host = "localhost";
        final int port = 9090;
        Client client = new Client(host, port);

        for (int i = 0; i < 5; i++) {
            String key = "key" + i;
            String value = "value" + i;
            client.setCommand("set", key, value, value.getBytes().length);  
            client.setCommand("get", key, null, 0);
        }
    }
}

