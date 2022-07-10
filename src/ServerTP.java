import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ServerTP extends Thread{

    private ArrayList<ClientHandler> clientsOnline = new ArrayList<>();
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private ArrayList<ClientHandler> opedClients = new ArrayList<>();
    private ArrayList<ClientHandler> bannedClients = new ArrayList<>();
    private ArrayList<String> pseudos = new ArrayList<>();
    private ArrayList<String> onlinePseudos = new ArrayList<>();
    private ArrayList<String> coInfos = new ArrayList<>();
    private ArrayList<String> ops = new ArrayList<>();
    private ArrayList<String> nonOps = new ArrayList<>();
    private ArrayList<String> bannedPseudos = new ArrayList<>();
    private static FileWriter fileWriter;
    ServerSocket serverSocket;
    File opsFile = new File("ops.txt");
    File nonOpFile = new File("non-ops.txt");
    File bannedFile = new File("banned.txt");
    @Override
    public void run() {
        try{
            serverSocket = new ServerSocket(8888);
            System.out.println("[LOG] Server started");
            fileWriter.write("[LOG] Server started\n");
            while(!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
            }
        } catch (IOException e){

            try {
                fileWriter.write("[ERROR] Closing server due to an SocketException...\n");
                fileWriter.flush();
                fileWriter.close();
                serverSocket.close();
                System.exit(69);
            } catch (IOException ex) {
                try {
                    serverSocket.close();
                    System.exit(69);
                } catch (IOException exc) {
                    System.exit(69);
                }
            }

        }
    }

    public ServerTP() throws IOException {
        if(!opsFile.exists()){
            opsFile.createNewFile();
            PrintWriter printWriter = new PrintWriter(opsFile);
            printWriter.println("Admin:Mric.21000");
            printWriter.flush();
            printWriter.close();
        }
        if(opsFile.isDirectory()){
            System.err.println("ops.txt est un dossier");
            System.exit(69);
        }
        nonOpFile = new File("non-ops.txt");
        if(!nonOpFile.exists()){
            nonOpFile.createNewFile();
        }
        if(nonOpFile.isDirectory()) {
            System.out.println("[ERROR] Non-op file is a directory");
            fileWriter.write("[ERROR] No-op file is a directory\n");
            return;
        }
        if(!bannedFile.exists()){
            bannedFile.createNewFile();
        }
        if(bannedFile.isDirectory()) {
            System.out.println("[ERROR] Banned file is a directory");
            fileWriter.write("[ERROR] Banned file is a directory\n");
            return;
        }
        reload();
        //coInfos.put("Admin","Mric.21000Dijon@college.com");
    }

    public static void main(String[] args) throws IOException {
        File server_logs_dir_path = new File("ServerLogs");
        server_logs_dir_path.mkdir();
        Date date = new Date();
        File log_file = new File(server_logs_dir_path.getName()+"/"+date.getTime()+".txt");
        fileWriter = new FileWriter(log_file);
        fileWriter.flush();
        new ServerTP().run();
    }

    public void broadcastMsg(String msg) throws IOException {
        for(ClientHandler client : clientsOnline){
            client.sendMsg(msg);
        }
        //System.out.println(clientsOnline.size());
        System.out.println("[LOG] "+msg);
        fileWriter.write("[LOG] "+msg+"\n");
        fileWriter.flush();
    }

    private class ClientHandler extends Thread{
        private PrintWriter out;
        private BufferedReader in;
        private Socket client;
        private String username;
        private String password;
        private String infosDeCo;
        private boolean isAdmin = false;
        public ClientHandler(Socket client){
            this.client = client;
        }

        private boolean isCorrect(String infos) {
            if(bannedPseudos.contains(infos.split(":")[0])){
                out.println("You're banned from this chat !");
                return false;
            }
            if(coInfos.contains(infos)){
                if(!onlinePseudos.contains(infos.split(":")[0])){
                    return true;
                }
                out.println("Someone with your username is already connected !");
                return false;
            }
            out.println("Incorrect login infos !");
            return false;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Username :");
                String askedPseudo = in.readLine();
                out.println("Password :");
                String askedPassword = in.readLine();
                if(askedPseudo.contains(":")||askedPassword.contains(":")){
                    closeEverything();
                    return;
                }
                infosDeCo = askedPseudo+":"+askedPassword;
                if(!isCorrect(infosDeCo)){
                    closeEverything();
                    return;
                }
                username = askedPseudo;
                password = askedPassword;
                if(ops.contains(username)){
                    isAdmin=true;
                    if(!opedClients.contains(this)) opedClients.add(this);
                }
                clientsOnline.add(this);
                if(!clients.contains(this)) clients.add(this);
                if(!pseudos.contains(this)) pseudos.add(this.username);
                onlinePseudos.add(this.username);
                broadcastMsg(username+" joined the chat !");
                String msg;
                while((msg=in.readLine())!=null){
                    if(!isCommand(msg)){
                        broadcastMsg(username+": "+msg);
                    }
                }
                closeEverything();
            } catch (IOException e) {
                try {
                    closeEverything();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void sendMsg(String msg){
            out.println(msg);
        }

        private boolean isCommand(String msg) throws IOException {
            if(isAdmin) {
                if(msg.startsWith("/stop")){
                    closeEverything();
                    fileWriter.write("[LOG] Server stopped\n");
                    fileWriter.flush();
                    fileWriter.close();
                    serverSocket.close();
                    return true;
                }
                if(msg.startsWith("/reload")){
                    reload();
                    return true;
                }
                if(msg.startsWith("/op ")){
                    if(onlinePseudos.contains(msg.split(" ")[1])){
                        for(ClientHandler client : clientsOnline){
                            if(client.username.equalsIgnoreCase(msg.split(" ")[1])){
                                client.isAdmin = true;
                                opedClients.add(client);
                                PrintWriter printWriter = new PrintWriter(new FileWriter(opsFile));
                                for (ClientHandler opedClient : opedClients){
                                    printWriter.println(opedClient.username+":"+client.password);
                                }
                                printWriter.println(client.username+":"+client.password);
                                printWriter.flush();
                                printWriter.close();
                                broadcastMsg(client.username+" is now op !");
                                fileWriter.write("[LOG] "+client.username+" is now op !\n");
                                return true;
                            }
                        }
                        //System.out.println("[LOG] Cannot op "+msg.split(" ")[1]+" !");
                        broadcastMsg("Cannot op "+msg.split(" ")[1]+" !");
                        fileWriter.write("[LOG] Cannot op "+msg.split(" ")[1]+" !\n");
                        return true;
                    }
                    //System.out.println("[LOG] "+msg.split(" ")[1]+" not connected !");
                    broadcastMsg(msg.split(" ")[1]+" not connected !");
                    fileWriter.write("[LOG] Cannot op "+msg.split(" ")[1]+" !\n");
                    return true;
                }
                if(msg.startsWith("/ban ")){
                    if(bannedPseudos.contains(msg.split(" ")[1])){
                        broadcastMsg(bannedPseudos+" est déjà banni(e) !");
                        return true;
                    }
                    bannedPseudos.add(msg.split(" ")[1]);
                    for(ClientHandler client : clients){
                        if(client.username.equalsIgnoreCase(msg.split(" ")[1])){
                            bannedClients.add(client);
                            client.out.println("You're banned !");
                            client.in.close();
                            client.out.close();
                            client.client.close();
                            return true;
                        }
                    }
                }

                if(msg.startsWith("/unban ")){
                    String pseudo = msg.split(":")[1];
                    if(bannedPseudos.contains(pseudo)){
                        bannedPseudos.remove(pseudo);
                    }
                    BufferedReader bannedFileReader = new BufferedReader(new FileReader(bannedFile));
                    ArrayList<String> bannedFileContent = new ArrayList<>();
                    String line;
                    while((line=bannedFileReader.readLine())!=null){
                        bannedFileContent.add(line);
                    }
                    String oldBannedInfos = "";
                    for(String details : bannedFileContent){
                        if(details.startsWith(pseudo+":")){
                            oldBannedInfos = details;
                            break;
                        }
                    }
                    if(!oldBannedInfos.equals("")){
                        bannedFileContent.remove(oldBannedInfos);
                        PrintWriter bannedFileWriter = new PrintWriter(new FileWriter(bannedFile));
                        for(String bannedUser : bannedFileContent){
                            bannedFileWriter.println(bannedUser);
                        }
                        bannedFileWriter.close();
                        broadcastMsgToOps(oldBannedInfos.split(":")[0]+" unbanned !");
                        return true;
                    }
                    broadcastMsgToOps(pseudo+" n'est pas banni(e) !");
                    return true;
                }

                if(msg.startsWith("/add ")){
                    if(!msg.contains(":")){
                        out.println("Erreur de syntaxe");
                        return true;
                    }
                    BufferedReader reader = new BufferedReader(new FileReader(nonOpFile));
                    ArrayList<String> nonOpPseudos = new ArrayList<>();
                    ArrayList<String> nonOpClients = new ArrayList<>();
                    String content;
                    while((content=reader.readLine())!=null){
                        nonOpPseudos.add(content.split(":")[0]);
                        nonOpClients.add(content);
                    }
                    reader.close();
                    if(nonOpPseudos.contains(msg.split(" ")[1].split(":")[0])){
                        out.println("Ce joueur a déjà été ajouté !");
                        return true;
                    }
                    PrintWriter printWriter = new PrintWriter(new FileWriter(nonOpFile));
                    for(String nonOpClient : nonOpClients){
                        printWriter.println(nonOpClient);
                    }
                    printWriter.println(msg.split(" ")[1]);
                    printWriter.flush();
                    printWriter.close();
                    return true;
                }

                if(msg.startsWith("/deop ")){
                    String user = msg.split(" ")[1];
                    BufferedReader opsBufferedReader = new BufferedReader(new FileReader(opsFile));
                    ArrayList<String> opsFileContent = new ArrayList<>();
                    String tempContent;
                    while((tempContent=opsBufferedReader.readLine())!=null){
                        opsFileContent.add(tempContent);
                    }
                    opsBufferedReader.close();

                    ops.clear();
                    for(String op : opsFileContent){
                        ops.add(op.split(":")[0]);
                    }

                    PrintWriter opsPW = new PrintWriter(new FileWriter(opsFile),true);
                    for(String op : opsFileContent){
                        opsPW.println(op);
                    }
                    opsPW.close();


                    BufferedReader nonOpsBufferedReader = new BufferedReader(new FileReader(nonOpFile));
                    ArrayList<String> nonOpsFileContent = new ArrayList<>();
                    while((tempContent=opsBufferedReader.readLine())!=null){
                        nonOpsFileContent.add(tempContent);
                    }
                    nonOpsBufferedReader.close();

                    nonOps.clear();
                    for(String nonOp : nonOpsFileContent){
                        nonOps.add(nonOp.split(":")[0]);
                    }

                    PrintWriter nonOpsPW = new PrintWriter(new FileWriter(nonOpFile),true);
                    for(String nonOp : nonOpsFileContent){
                        nonOpsPW.println(nonOp);
                    }
                    nonOpsPW.close();


                }

                if(msg.startsWith("/removeuser ")){
                    String user = msg.split(" ")[1];
                    if(user.contains(":")){
                        out.println("Le pseudo ne peut pas contenir de ':' !");
                        return true;
                    }
                    BufferedReader br = new BufferedReader(new FileReader(nonOpFile));
                    String content;
                    ArrayList<String> fileContent = new ArrayList<>();
                    while((content = br.readLine())!=null){
                        fileContent.add(content);
                    }
                    br.close();
                    for(String anUser : fileContent){
                        if(anUser.split(":")[0].equals(user)){
                            fileContent.remove(anUser);
                            PrintWriter pw = new PrintWriter(new FileWriter(nonOpFile));
                            for(String a : fileContent){
                                pw.println(a);
                            }
                            pw.flush();
                            pw.close();
                            out.println("User "+user+" deleted !");
                            return true;
                        }
                    }
                    return true;
                }
            }
            if(msg.startsWith("/list")){
                out.println("Il y a "+ clientsOnline.size()+" personnes en ligne :");
                for(String onlineClient : onlinePseudos){
                    out.println("- "+onlineClient);
                }
                return true;
            }
            return false;
        }

        public void closeEverything() throws IOException {
            try{
                out.close();
                in.close();
                client.close();
                if(username!=null){
                    broadcastMsg(username+" leaves the chat !");
                }
                onlinePseudos.remove(this.username);
                clientsOnline.remove(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastMsgToOps(String msg) throws IOException {
        for(ClientHandler client : clientsOnline){
            if(ops.contains(client.username)){
                client.isAdmin = true;
                client.sendMsg(msg);
            }
        }
        System.out.println(msg);
        fileWriter.write("[ADMIN BROADCAST] "+msg+"\n");
    }

    private void reload() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(opsFile));
        coInfos.clear();
        String infosDeCo;
        for(ClientHandler clientHandler : clients){
            clientHandler.isAdmin = false;
        }
        ops.clear();
        while((infosDeCo=bufferedReader.readLine())!=null){
            if(!coInfos.contains(infosDeCo)){
                coInfos.add(infosDeCo);
            }

            ops.add(infosDeCo.split(":")[0]);
        }

        for(ClientHandler clientHandler : clients){
            if(ops.contains(clientHandler.username)){
                clientHandler.isAdmin=true;
                if(!opedClients.contains(clientHandler)) opedClients.add(clientHandler);
            }
        }
        nonOps.clear();
        ArrayList<String> nonOpFileContent = new ArrayList<>();
        BufferedReader bufferedReader2 = new BufferedReader(new FileReader(nonOpFile));
        while((infosDeCo=bufferedReader2.readLine())!=null){
            if(!coInfos.contains(infosDeCo)) {
                coInfos.add(infosDeCo);
            }
            nonOpFileContent.add(infosDeCo);
            if(!nonOps.contains(infosDeCo.split(":")[0])) nonOps.add(infosDeCo.split(":")[0]);
        }
        bufferedReader.close();
        ArrayList<String> tempNonOp = new ArrayList<>();
        for(String nonOp : nonOps){
            if(ops.contains(nonOp)){
                tempNonOp.add(nonOp);
            }
        }

        nonOps.removeAll(tempNonOp);

        PrintWriter printWriter = new PrintWriter(new FileWriter(nonOpFile));
        for(String nonOp : nonOps){
            printWriter.println(nonOp);
        }
        printWriter.flush();
        printWriter.close();

        BufferedReader bannedFileReader = new BufferedReader(new FileReader(bannedFile));
        bannedPseudos.clear();
        String bannedReader;
        while((bannedReader = bannedFileReader.readLine())!=null){
            String tempPseudo = bannedReader.split(":")[0];
            bannedPseudos.add(tempPseudo);
            if(ops.contains(tempPseudo)) ops.remove(tempPseudo);
            if(nonOps.contains(tempPseudo)) nonOps.remove(tempPseudo);
        }
        bannedFileReader.close();

        broadcastMsgToOps("Config reloaded !");

        broadcastMsgToOps("Ops are :");
        for (String op : ops){
            broadcastMsgToOps("- "+op);
        }

        broadcastMsgToOps("Banned are :");
        for(String banned : bannedPseudos){
            broadcastMsgToOps("- "+banned);
        }

        broadcastMsgToOps("Other allowed peoples :");
        for(String nonOp : nonOps){
            broadcastMsgToOps("- "+nonOp);
        }
    }

}
