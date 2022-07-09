import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args) {
        new Client().run();
    }

    @Override
    public void run() {
        try{
            socket = new Socket("90.100.158.159",8888);
            out = new PrintWriter(socket.getOutputStream(),true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(new MsgHandler()).start();

            String msg;
            while((msg=in.readLine())!=null){
                if(msg.equals("/quit")) System.exit(69);
                System.out.println(msg);
            }
            System.exit(69);
        } catch (IOException e){
            try {
                if(socket!=null) socket.close();
                in.close();
                out.close();
                System.exit(69);
            } catch (Exception ex) {
                System.exit(69);
            }
        }
    }

    class MsgHandler implements Runnable{

        @Override
        public void run() {
            try{
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while(socket.isConnected()){
                    String msg = inReader.readLine();
                    if(msg.equalsIgnoreCase("/quit")){
                        System.out.println("Leaving...");
                        inReader.close();
                        System.exit(69);
                    } else{
                        out.println(msg);
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
