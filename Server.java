import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class Server {
    private ServerSocket SS;
        private final int Port 8090;
    public static void main(String[] args) {
        new Server().StartServer(5000);
    }

    private void StartServer(int Port) {
        try {
            SS=new ServerSocket(Port);
            System.out.println("Server On.");
            WaitForConection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void WaitForConection() {
        try {
            while(true){
                ProcessRequest(SS.accept());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ProcessRequest(Socket Client) {
        try {
            ObjectInputStream dis=new ObjectInputStream(Client.getInputStream());
            Movement m=(Movement)dis.readObject();
            if(m.Type==0)
                SetNewGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SetNewGame() {
        //Generate boards
        
        //Boolean [10][10][2] board;

    }
}
