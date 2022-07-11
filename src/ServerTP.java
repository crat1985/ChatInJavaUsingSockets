import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class ServerTP extends Thread{

    private ArrayList<ClientHandler> clientsOnline = new ArrayList<>();
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private ArrayList<ClientHandler> opedClients = new ArrayList<>();
    private ArrayList<ClientHandler> bannedClients = new ArrayList<>();
    private ArrayList<String> pseudos = new ArrayList<>();
    private ArrayList<String> onlinePseudos = new ArrayList<>();
    private ArrayList<String> coInfos = new ArrayList<>();
    private ArrayList<String> opsPseudos = new ArrayList<>();
    private ArrayList<String> nonOpsPseudos = new ArrayList<>();
    private ArrayList<String> bannedPseudos = new ArrayList<>();
    private ArrayList<String> opFileContent = new ArrayList<>();
    private ArrayList<String> nonOpFileContent = new ArrayList<>();
    private ArrayList<String> bannedFileContent = new ArrayList<>();
    private static PrintWriter printWriter;
    private ServerSocket serverSocket;
    private final File opsFile = new File("src/ops.txt");
    private final File nonOpFile = new File("src/non-ops.txt");
    private final File bannedFile = new File("src/banned.txt");
    @Override
    public void run() {
        try{
            serverSocket = new ServerSocket(8888);
            System.out.println("[LOG] Server started");
            printWriter.println("[LOG] Server started");
            while(!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
            }
        } catch (IOException e){

            try {
                printWriter.println("[ERROR] Closing server due to an SocketException...\n");
                printWriter.close();
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
        if(!new File("src").exists()){
            new File("src").mkdir();
        }
        if(!opsFile.exists()){
            opsFile.createNewFile();
            PrintWriter printWriter = new PrintWriter(new FileWriter(opsFile),true);
            printWriter.println("Admin:Mric.21000");
            printWriter.close();
        }
        if(opsFile.isDirectory()){
            System.out.println("[ERROR] Op file is a directory");
            printWriter.println("[ERROR] Op file is a directory");
            return;
        }
        if(!nonOpFile.exists()){
            nonOpFile.createNewFile();
        }
        if(nonOpFile.isDirectory()) {
            System.out.println("[ERROR] Non-op file is a directory");
            printWriter.println("[ERROR] No-op file is a directory");
            return;
        }
        if(!bannedFile.exists()){
            bannedFile.createNewFile();
        }
        if(bannedFile.isDirectory()) {
            System.out.println("[ERROR] Banned file is a directory");
            printWriter.println("[ERROR] Banned file is a directory");
            return;
        }
        reload();
    }

    public static void main(String[] args) throws IOException {
        File server_logs_dir_path = new File("ServerLogs");
        server_logs_dir_path.mkdir();
        Date date = new Date();
        File log_file = new File(server_logs_dir_path.getAbsolutePath()+"/"+date.getTime()+".txt");
        printWriter = new PrintWriter(new FileWriter(log_file),true);
        new ServerTP().run();
    }

    public void broadcastMsg(String msg) throws IOException {
        for(ClientHandler client : clientsOnline){
            client.sendMsg(msg);
        }
        //System.out.println(clientsOnline.size());
        System.out.println("[LOG] "+msg);
        printWriter.println("[LOG] "+msg);
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
                String askedPseudo = "";
                String askedPassword = "";
                while(true){
                    out.println("Username :");
                    askedPseudo = in.readLine();
                    out.println("Password :");
                    askedPassword = in.readLine();
                    if(askedPseudo==null){
                        return;
                    }
                    if(askedPassword==null){
                        return;
                    }
                    if(askedPseudo.contains(":")||askedPassword.contains(":")){
                        continue;
                    }
                    infosDeCo = askedPseudo+":"+askedPassword;
                    if(isCorrect(infosDeCo)) {
                        break;
                    }
                }
                username = askedPseudo;
                password = askedPassword;
                if(opsPseudos.contains(username)){
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
                    printWriter.println("[LOG] Server stopped");
                    printWriter.close();
                    serverSocket.close();
                    return true;
                }
                if(msg.startsWith("/reload")){
                    reload();
                    return true;
                }
                if(msg.startsWith("/op ")){
                    opUser(msg);
                    return true;
                }
                if(msg.startsWith("/ban ")){
                    banUser(msg);
                    return true;
                }

                if(msg.startsWith("/unban ")){
                    unbanUser(msg);
                    return true;
                }

                if(msg.startsWith("/adduser ")){
                    String infos = msg.split(" ")[1];
                    String pseudo = infos.split(":")[0];
                    String password = infos.split(":")[1];
                    if(!msg.contains(":")){
                        out.println("Erreur de syntaxe");
                        return true;
                    }
                    reload();
                    if(nonOpFileContent.contains(infos)){
                        out.write("User already exists as non-op user !");
                        return true;
                    }
                    for(String nonOpUser : nonOpFileContent){
                        if(nonOpUser.startsWith(pseudo+":")){
                            out.write("User already exists as non-op user but with a different password !");
                            return true;
                        }
                    }
                    if(opFileContent.contains(infos)){
                        out.write("User already exists as op user !");
                        return true;
                    }
                    for(String opUser : opFileContent){
                        if(opUser.startsWith(pseudo+":")){
                            out.write("User already exists as op user but with a different password !");
                            return true;
                        }
                    }
                    nonOpFileContent.add(infos);
                    PrintWriter printWriter = new PrintWriter(new FileWriter(nonOpFile),true);
                    for(String nonOpUser : nonOpFileContent){
                        printWriter.println(nonOpUser);
                    }
                    printWriter.close();
                    reload();
                    return true;
                }

                if(msg.startsWith("/deop ")){
                    deopUser(msg);
                    return true;
                }

                if(msg.startsWith("/removeuser ")){
                    removeUser(msg);
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

        private void removeUser(String msg) throws IOException {
            String pseudo = msg.split(" ")[1];
            if(pseudo.equals("Admin")){
                out.println("Cannot remove user Admin !");
                return;
            }
            if(pseudo.contains(":")){
                out.println("Username cannot contains ':' !");
                return;
            }
            reload();

            for(String info : opFileContent){
                if(info.startsWith(pseudo+":")){
                    deopUser("/deop "+pseudo);
                    break;
                }
            }
            String nonOpUser = null;
            for(String nonOpInfo : nonOpFileContent){
                if(nonOpInfo.startsWith(pseudo+":")){
                    nonOpUser = nonOpInfo;
                    break;
                }
            }
            if(nonOpUser!=null){
                nonOpFileContent.remove(nonOpUser);
                PrintWriter nonOpFileWriter = new PrintWriter(new FileWriter(nonOpFile) ,true);
                for(String user : nonOpFileContent){
                    nonOpFileWriter.println(user);
                }
                nonOpFileWriter.close();
                reload();
            }
        }

        private void opUser(String msg) throws IOException {
            String pseudo = msg.split(" ")[1];
            if(pseudo.contains(":")) {
                out.println("Un pseudo ne peut pas contenir ':' !");
                return;
            }
            if(pseudo.length()<1){
                out.println("Un pseudo ne peut pas faire moins de 1 caractère XD");
                return;
            }
            reload();
            boolean mustBeOpped = false;
            for(String opUser : opFileContent){
                if(opUser.startsWith(pseudo+":")){
                    out.println(pseudo+" is already op !");
                    return;
                }
            }
            String tempInfos = null;
            for(String nonOpUser : nonOpFileContent){
                if(nonOpUser.startsWith(pseudo+":")){
                    mustBeOpped = true;
                    tempInfos = nonOpUser;
                }
            }
            for(String bannedUser : bannedFileContent){
                if(bannedUser.startsWith(pseudo+":")){
                    mustBeOpped = false;
                    out.println(pseudo+" is banned so he/she can't be opped !");
                    return;
                }
            }
            if(!mustBeOpped){
                out.println("User "+pseudo+" not found !");
                return;
            }
            nonOpFileContent.remove(tempInfos);
            PrintWriter nonOpWriter = new PrintWriter(new FileWriter(nonOpFile),true);
            for(String nonOpUser : nonOpFileContent){
                nonOpWriter.println(nonOpUser);
            }
            nonOpWriter.close();
            PrintWriter opWriter = new PrintWriter(new FileWriter(opsFile), true);
            for(String opUser : opFileContent){
                opWriter.println(opUser);
            }
            opWriter.close();
            reload();
        }

        private void deopUser(String msg) throws IOException {
            String pseudo = msg.split(" ")[1];
            reload();

            String infos = "";
            boolean isOp = false;

            for(String nonOpUser : nonOpFileContent){
                if(nonOpUser.startsWith(pseudo+":")){
                    if(!opFileContent.contains(nonOpUser)){
                        out.println(pseudo+" is not oped !");
                        return;
                    }
                    infos = nonOpUser;
                    isOp=true;
                }
            }
            for(String opUser : opFileContent){
                if(opUser.startsWith(pseudo+":")){
                    infos = opUser;
                    isOp=true;
                }
            }
            if(isOp){
                nonOpFileContent.remove(infos);
                PrintWriter nonOpFileWriter = new PrintWriter(new FileWriter(nonOpFile) ,true);
                for(String nonOpUser : nonOpFileContent){
                    nonOpFileWriter.println(nonOpUser);
                }
                nonOpFileWriter.close();
                reload();
                return;
            }
            out.println(pseudo+" not found !");
        }

        private void banUser(String msg) throws IOException {
            String infos = msg.split(" ")[1];
            reload();
            for(String info : bannedFileContent){
                if(info.startsWith(infos+":")){
                    broadcastMsgToOps(infos.split(":")[0]+" is already banned !");
                    return;
                }
            }
            boolean isOp = false;
            boolean isNonOp = false;
            String opAndNonOpinfo = null;
            for(String info : opFileContent){
                if(info.startsWith(infos+":")){
                    isOp=true;
                    opAndNonOpinfo = info;
                    break;
                }
            }
            for(String info : nonOpFileContent){
                if(info.startsWith(infos+":")){
                    isNonOp=true;
                    opAndNonOpinfo = info;
                    break;
                }
            }
            if(isOp&&isNonOp){
                nonOpFileContent.remove(opAndNonOpinfo);
                PrintWriter nonOpFileWriter = new PrintWriter(new FileWriter(nonOpFile),true);
                for(String line : nonOpFileContent){
                    nonOpFileWriter.println(line);
                }
                nonOpFileWriter.close();
            }

            if(opAndNonOpinfo==null){
                broadcastMsgToOps(infos+" not found in config files !");
                return;
            }
            bannedFileContent.add(opAndNonOpinfo);
            PrintWriter bannedFileWriter = new PrintWriter(new FileWriter(bannedFile),true);
            for(String bannedInfo : bannedFileContent){
                bannedFileWriter.println(bannedInfo);
            }
            bannedFileWriter.close();
            broadcastMsgToOps(infos+" was banned !");
        }

        private void unbanUser(String msg) throws IOException {
            reload();
            String pseudo = msg.split(" ")[1];
            String oldBannedInfos = "";
            for(String info : bannedFileContent){
                if(info.startsWith(pseudo+":")){
                    oldBannedInfos = info;
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
                return;
            }
            broadcastMsgToOps(pseudo+" isn't banned !");
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
            if(opsPseudos.contains(client.username)){
                client.isAdmin = true;
                client.sendMsg(msg);
            }
        }
        System.out.println(msg);
        printWriter.println("[ADMIN BROADCAST] "+msg);
    }

    private void reload() throws IOException {
        coInfos.clear();
        String infosDeCo;
        for(ClientHandler clientHandler : clients){
            clientHandler.isAdmin = false;
        }
        opsPseudos.clear();
        opFileContent.clear();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(opsFile));
        while((infosDeCo=bufferedReader.readLine())!=null){
            if(!coInfos.contains(infosDeCo)){
                coInfos.add(infosDeCo);
            }
            opFileContent.add(infosDeCo);
            if(!opsPseudos.contains(infosDeCo.split(":")[0])) opsPseudos.add(infosDeCo.split(":")[0]);
        }
        nonOpsPseudos.clear();
        nonOpFileContent.clear();
        BufferedReader bufferedReader2 = new BufferedReader(new FileReader(nonOpFile));
        while((infosDeCo=bufferedReader2.readLine())!=null){
            if(!coInfos.contains(infosDeCo)) {
                coInfos.add(infosDeCo);
            }
            nonOpFileContent.add(infosDeCo);
            if((!nonOpsPseudos.contains(infosDeCo.split(":")[0]))&&(!opsPseudos.contains(infosDeCo.split(":")[0]))) nonOpsPseudos.add(infosDeCo.split(":")[0]);
        }
        bufferedReader2.close();

        PrintWriter printWriter = new PrintWriter(new FileWriter(nonOpFile),true);
        for(String nonOp : nonOpFileContent){
            printWriter.println(nonOp);
        }
        printWriter.close();

        BufferedReader bannedFileReader = new BufferedReader(new FileReader(bannedFile));
        bannedPseudos.clear();
        bannedFileContent.clear();
        String bannedReader;
        while((bannedReader = bannedFileReader.readLine())!=null){
            String tempPseudo = bannedReader.split(":")[0];
            bannedPseudos.add(tempPseudo);
            bannedFileContent.add(bannedReader);
        }
        bannedFileReader.close();

        broadcastMsgToOps("Config reloaded !");

        broadcastMsgToOps("Ops are :");
        for (String op : opsPseudos){
            broadcastMsgToOps("- "+op);
        }

        broadcastMsgToOps("Banned are :");
        for(String banned : bannedPseudos){
            broadcastMsgToOps("- "+banned);
        }

        broadcastMsgToOps("Other allowed peoples :");
        for(String nonOp : nonOpsPseudos){
            broadcastMsgToOps("- "+nonOp);
        }
    }

}
