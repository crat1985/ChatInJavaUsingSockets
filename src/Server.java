import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private ArrayList<ConnectionHandler> connections;
    private ExecutorService pool;

    public Server(){
        connections = new ArrayList<>();
    }

    public static void main(String[] args) {
        new Server().run();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            pool = Executors.newCachedThreadPool();
            while(!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket);
                connections.add(handler);
                pool.execute(handler);
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String msg){
        for(ConnectionHandler client : connections){
            if(client!=null) {
                client.sendMsg(msg);
            }
        }
        System.out.println(msg);
    }



    class ConnectionHandler implements Runnable{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                broadcast(nickname+" connected");
                String msg;
                while((msg=in.readLine())!=null){
                    if(msg.startsWith("/nick ")){
                        String[] msgSplit = msg.split(" ");
                        if(msgSplit.length>=2){
                            broadcast(nickname+" s'est renommé en "+msgSplit[1]);
                            nickname = msgSplit[1];
                        }
                    } else if (msg.equalsIgnoreCase("/quit")) {
                        broadcast(nickname+" left the chat!");
                        connections.remove(this);
                        in.close();
                        out.close();
                        socket.close();
                    } else if (msg.startsWith("/help")) {
                        out.println("List of commands :");
                        out.println("- /nick <new pseudo>: rename in another pseudo");
                        out.println("- /help: display this help");
                        out.println("- /list: display the number of peoples connected");
                        out.println("- /quit: leave the chat");
                    } else if (msg.startsWith("/list")) {
                        this.sendMsg("Nombre de connectés : "+connections.size());
                    } else {
                        broadcast(nickname + ": " + msg);
                    }
                }
                connections.remove(this);
                broadcast(nickname+" left the chat");
            } catch (IOException e){
                connections.remove(this);
                broadcast(nickname+" left the chat");
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
                } catch (IOException ex) {

                }
            }
        }

        public void sendMsg(String msg){
            out.println(msg);
        }
    }
}