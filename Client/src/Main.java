import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 9999);

        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println("Hello server!");
        System.out.println(in.readLine());
    }
}
