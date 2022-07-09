import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server2 implements Runnable{
    private ArrayList<ConnectionHandler> connections;

    public Server2(){
        connections = new ArrayList<>();
    }

    public static void main(String[] args) {
        new Server2().run();
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
        private boolean isAdmin = false;
        private String IP;
        private static ArrayList<String> bannedIPs = new ArrayList<>();
        static ArrayList<String> pseudos = new ArrayList<>();

        public ConnectionHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                IP = socket.getInetAddress().toString();
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                if(nickname.equals("Admin:Mric.21000Dijon@college.com")){
                    isAdmin = true;
                    nickname = "Admin";
                }
                if(pseudos.contains(nickname)){
                    out.println("Quelqu'un avec ce pseudo est déjà connecté");
                    connections.remove(this);
                    in.close();
                    out.close();
                    socket.close();
                    return;
                }
                if(bannedIPs.contains(IP)){
                    out.println("You're banned !");
                    broadcast(nickname+" tried to connect but he/she is banned !");
                    connections.remove(this);
                    in.close();
                    out.println("/quit");
                    out.close();
                    socket.close();
                }
                broadcast(nickname+" connected");
                pseudos.add(nickname);
                //System.out.println(pseudos.get(pseudos.size()-1));
                String msg;
                while((msg=in.readLine())!=null){
                    if(!isAdmin && msg.startsWith("/nick ")){
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
                    } else if (isAdmin) {
                        if(msg.startsWith("/ban ")){
                            if(msg.split(" ").length>=2){
                                String tempPseudo = msg.split(" ")[1];
                                if(tempPseudo.equalsIgnoreCase("admin")) out.println("Cannot ban Admin !");
                                 else if(pseudos.contains(tempPseudo)){
                                    for(ConnectionHandler client : connections){
                                        if(client.nickname.equals(tempPseudo)){
                                            bannedIPs.add(client.IP);
                                            client.out.println("You're banned !");
                                            //broadcast(client.nickname+" tried to connect but he/she is banned !");
                                            broadcast(client.nickname+" banned !");
                                            broadcast(client.nickname+" disconnected");
                                            connections.remove(client);
                                            pseudos.remove(client.nickname);
                                            client.in.close();
                                            client.out.close();
                                            client.socket.close();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        broadcast(nickname + ": " + msg);
                    }
                }
                connections.remove(this);
                pseudos.remove(nickname);
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