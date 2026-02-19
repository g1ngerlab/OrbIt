import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(9999);
        System.out.println("Server started...");

        Socket client = server.accept();
        System.out.println("Client connected");

        BufferedReader in = new BufferedReader(
            new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);

        String msg;
        while ((msg = in.readLine()) != null) {
            System.out.println("Client: " + msg);
            out.println("Echo: " + msg);
        }
    }
}
