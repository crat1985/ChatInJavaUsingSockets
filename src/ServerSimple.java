import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerSimple implements Runnable{
    private ArrayList<ConnectionHandler> connections;
    public ServerSimple(){
        connections = new ArrayList<>();
    }
    public static void main(String[] args) {
        new Server().run();
    }
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            while(!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket);
                connections.add(handler);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void broadcast(String msg){
        for(ConnectionHandler client : connections) if(client!=null) client.sendMsg(msg);
        System.out.println(msg);

    }
    class ConnectionHandler implements Runnable{
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        public ConnectionHandler(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("Please enter a nickname: ");
                username = in.readLine();
                broadcast(username +" connected");
                String msg;
                while((msg=in.readLine())!=null){
                    if (msg.equalsIgnoreCase("/quit")) {
                        broadcast(username +" left the chat!");
                        connections.remove(this);
                        in.close();
                        out.close();
                        socket.close();
                    } else {
                        broadcast(username + ": " + msg);
                    }
                }
                connections.remove(this);
                broadcast(username +" left the chat");
            } catch (IOException e){
                connections.remove(this);
                broadcast(username +" left the chat");
                try{
                    if(in!=null){
                        in.close();
                    }
                    if(out!=null){
                        out.close();
                    }
                    if(socket!=null){
                        socket.close();
                    }
                } catch (IOException ex) {}
            }
        }
        public void sendMsg(String msg){out.println(msg);}
    }
}