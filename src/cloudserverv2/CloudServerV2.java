package cloudserverv2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudServerV2 {

    private static ServerSocket listener;

    public static void main(String[] args) {
        try {
            listener = new ServerSocket(19090); // listen on socket
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        while (true) {
            try {
                Socket socket = listener.accept(); // when anyone connects launch their activity on new thread
                new UserThread(socket).start();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static class UserThread extends Thread {

        private Socket socket;

        private static final int GETDATA = 0001;
        private static final int VERIFYCREDENTIALS = 0002;
        private static final int CREATE_NEW_USER = 0003;
        private static final int UPLOAD_FILE = 0004;

        private static final int DATA_CHUNK_SIZE = 102400;

        static final String DB_URL = "jdbc:sqlite:.\\CloudUsers.db"; // IF RUNNING CHANGE URL TO LINK TO FILE LOCATION ON HOST MACHINE

        public UserThread(Socket socket) { //link the socket connection
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())); // recieve command and creds
                String command = br.readLine();
                String[] credentials = {br.readLine(), br.readLine()};

                if (command.equals(GETDATA + "")) {                             // IF downloading data
                    String userDirPath = getAuthDirPath(credentials);           // authenticate
                    String jsonDataStr = br.readLine();                         // get the json code
                    try {
                        File selectedFile = new File(userDirPath + getFilePathFromJson(new JSONObject(new String(Files.readAllBytes(Paths.get(userDirPath + "\\FileList.json")))), jsonDataStr.toCharArray()));
                        sendData(selectedFile, socket, br);             //get the file using the json code
                    } catch (JSONException jsone) {
                        jsone.printStackTrace();
                    }

                } else if (command.equals(VERIFYCREDENTIALS + "")) {           // if we need to verify crednetials
                    System.out.println("Cred Verifying");
                    System.out.println(credentials[0] + ", " + credentials[1]); // check credentials and save if accessable
                    boolean accessable = authenticate(credentials);
                    sendAuth(accessable, socket);                           // send back operational or not
                    if (accessable) {
                        String userDirPath = getAuthDirPath(credentials);
                        System.out.println("Opening: " + userDirPath + "\\FileList.json"); 
                        File selectedFile = new File(userDirPath + "\\FileList.json"); 
                        sendData(selectedFile, socket, br);                         // send the Json file
                        System.out.println("Sent. Auth Complete");
                    }

                } else if (command.equals(CREATE_NEW_USER + "")) {                                    //Making a new user
                    System.out.println("Cred Maker Check");
                    System.out.println(credentials[0] + ", " + credentials[1]);                 // make sure the credentials are not in usage
                    boolean exists = userInDataBase(credentials[0]);
                    sendAuth(!exists, socket);
                    if (!exists) {
                        createNewUser(credentials);
                        String userDirPath = getAuthDirPath(credentials);
                        System.out.println("Opening: " + userDirPath + "\\FileList.json");      // send the Json File
                        File selectedFile = new File(userDirPath + "\\FileList.json");
                        sendData(selectedFile, socket, br);                             
                        System.out.println("Sent. Auth Complete");
                    }

                } else if (command.equals(UPLOAD_FILE + "")) {                // If uploading a file
                    String userDirPath = getAuthDirPath(credentials);
                    String jsonDataStr = br.readLine();
                    String fileName = br.readLine();
                    try {
                        File selectedFilePath = new File(userDirPath + getFilePathFromJson(new JSONObject(new String(Files.readAllBytes(Paths.get(userDirPath + "\\FileList.json")))), jsonDataStr.toCharArray()) + fileName);
                        saveData(selectedFilePath.getPath(), getUploadedData(socket));
                    } catch (JSONException jsone) {
                        jsone.printStackTrace();
                    }
                    if(!new JSONObject(new String(Files.readAllBytes(Paths.get(userDirPath + "\\FileList.json")))).toString().contains(fileName)) {
                        appendToJson(userDirPath, fileName, jsonDataStr);
                    }
                }
                br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (JSONException ex) {
                ex.printStackTrace();
            } 
        }

        public void sendAuth(boolean access, Socket socket) throws IOException {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBoolean(access);
            System.out.println("Access: " + access);
        }

        public void sendData(File selectedFile, Socket socket, BufferedReader br) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());

                ArrayList<byte[]> bytes = new ArrayList<>();
                for (int i = 0; i < fileContent.length / DATA_CHUNK_SIZE; i++) {
                    byte[] temp = fileContent;
                    bytes.add(Arrays.copyOfRange(temp, i * DATA_CHUNK_SIZE, (i * DATA_CHUNK_SIZE) + DATA_CHUNK_SIZE));
                }
                byte[] temp = fileContent;
                bytes.add(Arrays.copyOfRange(temp, (temp.length / DATA_CHUNK_SIZE) * DATA_CHUNK_SIZE, temp.length));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                out.writeInt(bytes.size() - 1);
                for (int i = 0; i < bytes.size() - 1; i++) {
                    if (br.readLine().equals("req")) {
                        out.write(bytes.get(i));
                        System.out.println(i + " / " + (bytes.size() - 1));
                    }
                }
                if (br.readLine().equals("req")) {
                    out.writeInt(bytes.get(bytes.size() - 1).length);
                }
                System.out.println(bytes.get(bytes.size() - 1).length);
                if (br.readLine().equals("req")) {
                    out.write(bytes.get(bytes.size() - 1));
                }
                out.flush();
                socket.close();
            } catch (IOException ioe) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                ioe.printStackTrace();
            }
        }

        public void saveData(String selectedFile, ArrayList<byte[]> bytes) {
            try { // change stream to internal storage.
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(selectedFile));
                int offset = 0;
                byte[] concatenatedBytes = new byte[(((bytes.size() - 1) * DATA_CHUNK_SIZE) + bytes.get(bytes.size() - 1).length)];
                for (int i = 0; i < bytes.size(); i++) {
                    System.arraycopy(bytes.get(i), 0, concatenatedBytes, offset, bytes.get(i).length);
                    offset += bytes.get(i).length;
                }
                bos.write(concatenatedBytes);
                bos.flush();
                bos.close();
            } catch (IOException ioe) { // do not need fileNotFound since we are creating file
                ioe.printStackTrace();
            }
        }

        public ArrayList<byte[]> getUploadedData(Socket socket) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                ArrayList<byte[]> bytes = new ArrayList<>();
                int numOfChunks = 0;
                numOfChunks = in.readInt();
                byte[] tempBytes;
                for (int i = 0; i < numOfChunks; i++) {
                    out.println("req"); //request
                    tempBytes = new byte[DATA_CHUNK_SIZE];
                    in.readFully(tempBytes, 0, DATA_CHUNK_SIZE);
                    bytes.add(tempBytes);
                }
                out.println("req");
                tempBytes = new byte[in.readInt()];
                out.println("req");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                in.readFully(tempBytes, 0, tempBytes.length);
                bytes.add(tempBytes);
                out.println("clear");
                return bytes;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean authenticate(String[] credentials) {
            boolean result = false;
            try {
                Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                String sql = "SELECT COUNT(*) FROM Users WHERE USERNAME = '" + credentials[0] + "' AND PASSWORD = '" + credentials[1] + "'";
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                result = (rs.getInt("COUNT(*)") >= 1);
                rs.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        public String getAuthDirPath(String[] credentials) {
            String directory = "";
            try {
                Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                String sql = "SELECT DIRECTORY FROM Users WHERE USERNAME = '" + credentials[0] + "' AND PASSWORD = '" + credentials[1] + "'";
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                directory = rs.getString("DIRECTORY");
                rs.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return directory;
        }

        public void createNewUser(String[] credentials) {
            try {
                Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                String sql = "INSERT INTO Users VALUES('" + credentials[0] + "', '" + credentials[1] + "', 'Clients\\\\" + credentials[0] + "')";
                stmt.executeUpdate(sql);
                conn.close();
                new File("Clients\\" + credentials[0]).mkdirs();
                Files.write(new File("Clients\\" + credentials[0] + "\\FileList.json").toPath(), ("{\"name\" : \"" + credentials[0] + "\",\"files\" : [],\"folders\" :[]}").getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public boolean userInDataBase(String username) {
            boolean result = false;
            try {
                Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                String sql = "SELECT COUNT(*) FROM Users WHERE USERNAME = '" + username + "'";
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                result = (rs.getInt("COUNT(*)") >= 1);
                rs.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        public String getFilePathFromJson(JSONObject jo, char[] jsonGetStr) {
            if (jsonGetStr.length <= 0) {
                return "\\";
            } else if (jsonGetStr[0] == 'i') {
                try {
                    return "\\" + jo.getJSONArray("files").getString(Integer.parseInt(Character.toString(jsonGetStr[1])));
                } catch (JSONException jsone) {
                    jsone.printStackTrace();
                    return "";
                }
            }
            try {
                return "\\" + jo.getJSONArray("folders").getJSONObject(Integer.parseInt(Character.toString(jsonGetStr[1]))).getString("name") + getFilePathFromJson(jo.getJSONArray("folders").getJSONObject(Integer.parseInt(Character.toString(jsonGetStr[1]))), Arrays.copyOfRange(jsonGetStr, 2, jsonGetStr.length));
            } catch (JSONException jsone) {
                jsone.printStackTrace();
            }
            return "";
        }

        public void appendToJson(String userDirPath, String fileName, String jsonDataStr) {
            try {
                JSONObject tail = new JSONObject(new String(Files.readAllBytes(Paths.get(userDirPath + "\\FileList.json"))));
                JSONObject head = tail;
                for (int i = 0; i < jsonDataStr.length() / 2; i++) {
                    tail = tail.getJSONArray("folders").getJSONObject(Integer.parseInt(jsonDataStr.charAt((i * 2) + 1) + ""));
                }
               tail.getJSONArray("files").put(fileName);
               BufferedWriter bw = new BufferedWriter(new FileWriter(userDirPath + "\\FileList.json"));
               String temp = head.toString();
               bw.write(temp);
               bw.flush();
               bw.close();
            } catch (JSONException ex) {
                Logger.getLogger(CloudServerV2.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(CloudServerV2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}
