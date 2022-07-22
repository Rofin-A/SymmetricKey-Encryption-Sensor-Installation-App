/**
 * @classname: InstallerServer.Java
 * @author: Rofin A
 */
/*----------------------------------------------------------------------------- 
*  Purpose: This class serves as the server where it receives the details 
sent by installers. This class relies on the symmetric key and the credential
supplied by the installers to authenticate the installers. If the authentication
suceeds then the server evaluates the role of the installers to check if they 
have the previlages for the particular task. If the user have sufficient previlages
then the server parses the json and updsate the sensors location in its state log
and also generates a KML in the desktop with the co-ordinates of sensors installed
In case of any failure, server will not carry any update and will send back an
error message and close the connection.
-------------------------------------------------------------------------------*/
package SymmetricKeyEncryption;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;

public class InstallerServer {
//--------------------Global Variable Declaration-------------------------------

    private static String key;
    static HashMap<String, SensorInstaller> installer = new HashMap<>();
    static List<SensorLoc> loc = new ArrayList<SensorLoc>();
    static HashMap<Integer, SensorLoc> SensorMap = new HashMap<>();
    static int i = 0;
    public static void main(String args[]) {
        
//--------------------Establish Socket connection-------------------------------
// Create a socket connection at port 7896 and wait for request from client.
// accept the incoming messages from client and decipher it using algorithms
        try {
            //Capture the symmetric key
            Scanner usr_inp = new Scanner(System.in);
            System.out.println("Enter symmetric key as a 16-digit integer");
            key = usr_inp.nextLine();
            System.out.println("Waiting for installers to visit...");
            int serverPort = 7896; // the server port
            ServerSocket listenSocket = new ServerSocket(serverPort);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Connection c = new Connection(clientSocket, key);
                
//                try {
//                    c.join();
//                } catch (InterruptedException ex) {
//                    System.out.print("connection interuppted:" + ex.getMessage());
//                }

            }
        } catch (IOException e) {
            System.out.println("Listen socket:" + e.getMessage());
        }
    }

    public static void generate_kml() {

//--------------------------- Header for the KML -------------------------------
        String kml_header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<kml xmlns=\"http://earth.google.com/kml/2.2\">\n"
                + "<Document>\n"
                + " <Style id=\"style1\">\n"
                + " <IconStyle>\n"
                + " <Icon>\n"
                + " <href>https://lh3.googleusercontent.com/MSOuW3ZjC7uflJAMst-cykSOEOwI_cVz96s2rtWTN4-Vu1NOBw80pTqrTe06R_AMfxS2=w170</href>\n"
                + " </Icon>\n"
                + " </IconStyle>\n"
                + " </Style>";
        
//----------------------- Tag the locations of sensors -------------------------
        // Placemark - the co-ordinates need to be generated accordingly 
        String place = "<Placemark>\n"
                + " <name>Microphone %d</name>\n"
                + " <description>%s</description>\n"
                + " <styleUrl>#style1</styleUrl>\n"
                + " <Point>\n"
                + " <coordinates>%s</coordinates>\n"
                + " </Point>\n"
                + "</Placemark>";

//----------------------- End of kml file tag ------- -------------------------

        String eof = "</Document>\n"
                + "</kml>";

        
//-------------------------Build the kml file-------------------------------- 
//Tag the locations of sensor in KML, for all the sensonrs maintained in the 
//hashmap SensorMap 
        StringBuilder kml = new StringBuilder();
        kml.append(kml_header);

        //Loop through the hashmap to populate the placemark tag with locations
        for (Integer sensor : SensorMap.keySet()) {
            SensorLoc det = SensorMap.get(sensor);
            String co_ord = det.latitude + "," + det.longitude + "," + det.altitude;
            String loc_det = String.format(place, sensor, det.user, co_ord);
            kml.append(loc_det);
        }
        kml.append(eof);
        
//-------------------------Upload the KML file to the desktop-------------------        
        String filepath = System.getProperty("user.home") + "/Desktop/Sensors.kml";
        try {
            FileOutputStream kmlfile = new FileOutputStream(filepath);
            try {
                kmlfile.write(kml.toString().getBytes(), 0, kml.toString().length());
            } catch (IOException ex) {
                System.out.println("Could not write KML file" + ex.getMessage());
            }

        } catch (FileNotFoundException ex) {
            System.out.println("Could not open KML file" + ex.getMessage());
        }

    }

    
//-----------------Instantiate the chief and associate installers---------------
    
//The details of the installers and their credentials are held in an object of 
//InstallerServer abstract class. Based on their designation the objects will be 
//instantiated of more specialized classes: AssociateSensorInstaller,
//CheifSensorInstaller. In reall time this data will flow in from DB. Note:This 
// is a design decision to hold the data in object instance rather than in local
// datastructure    
    public void initialize_installer() {
        installer.put("Barack", new ChiefSensorInstaller("Barack", "CN58kxx9k3+d9MDpYCQ+vc/d9pKK9pin1sgdyioLbP8=", "HdnxGyf5FEC9sia17idcpg=="));
        installer.put("Hillary", new AssociateSensorInstaller("Hillary", "T1tJC8K/5d0Y6GkedDd7WC4qA7PuUEjshXbXfrN9ruQ=", "OKG++RuhJK1slVlt62u/Kg=="));
        installer.put("Donald", new AssociateSensorInstaller("Donald", "I72xM9UPcrLeYDmhVjz4PtAmtCUZRs7BLDU2T0shXb0=", "8Sf06V8NLHmPVUEERdXKCg=="));
    }

//----------------------------Authenticate Installers---------------------------

//This metho calls the method auth_check of the PasswordHash Utility program to 
//authenticate the installer    
    public boolean authenticate_installer(String user, String pass) {
        SensorInstaller ins = installer.get(user);
        if (ins != null) {
            PasswordHash ph = new PasswordHash();
            return (ph.auth_check(pass, ins.salt, ins.password));
            //return (pass.equals(ins.password));
        }
        return (false);
    }

   //------------------Capture the location details sent by the client----------
    public int update_location(String user, String latitude, String longitude, String altitude, String sid) {
        SensorInstaller ins = installer.get(user);
        if (ins instanceof ChiefSensorInstaller) {
            //populate the sensor details in HashMap
            //Cheif sensor installer can install and relocate the sensors
            SensorLoc old_val = SensorMap.put(Integer.parseInt(sid), new SensorLoc(user, latitude, longitude, altitude));
            if (old_val != null) {
                return 1;  //Installed for the first time
            } else {
                return 0;  //Sensor relocated
            }
        } else {
            if (SensorMap.containsKey(Integer.parseInt(sid))) {
                return 5;  //Authorization failure
            } else {
                loc.add(new SensorLoc(user, latitude, longitude, altitude));
                SensorMap.put(Integer.parseInt(sid), new SensorLoc(user, latitude, longitude, altitude));
                return 0; //Installed for the first time
            }
        }
    }
}
// Create a thread for each connection on the server object
class Connection extends Thread {
    
//----Global Variable declaration
//--------------------------------
    String key;
    String user = null;
    String pass = null;
    String sid = null;
    String latitude = null;
    String longitude = null;
    String altitude = null;

    boolean isValidKey;

    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;

//--------------------Establish connection as a thread---------------------------
    public Connection(Socket aClientSocket, String key) {
        this.key = key;
        try {
            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    public void run() {
        InstallerServer is = new InstallerServer();
        is.initialize_installer();
        try {			                 // an echo serve
           
            while (clientSocket.getInputStream().available() <= 0) {
            }
            isValidKey = decrypt_inc_cipher();
//-------------------
                
//--------------Parse the decrypted message for further processing--------------
                TEA encryption = new TEA(key.getBytes());
// Check if the client provided the symmetric key correct
                if (isValidKey) {
                    boolean authenticated = is.authenticate_installer(user, pass);
                    //authentication sucess
                    if (authenticated) {
//-----------------Capture the location details and hold it in the state of server                        
                        int ret_code = is.update_location(user, latitude, longitude, altitude, sid);

//        Get installers designation
                        String designation = null;
                        SensorInstaller ins = InstallerServer.installer.get(user);
                        if (ins instanceof ChiefSensorInstaller) {
                            designation = "Chief Sensor Installer";
                        } else {
                            designation = "Associate Sensor Installer";
                        }

//------Capture sensors location,upadte the hashmap,Generate the kml file-------                        
                        // location updated
                        switch (ret_code) {
                            case 1:
                                {
                                    System.out.println("Got visit " + (++InstallerServer.i) + " from " + user + ", " + designation + ", a sensor has been moved");
                                    String reply = "Thank you. The sensor’s new location was securely transmitted to GunshotSensing Inc.";
                                    byte[] msg = encryption.encrypt(reply.getBytes("US-ASCII"));
                                    out.write(msg);
                                    out.flush();
                                    //generate KML
                                    InstallerServer.generate_kml();
                                    break;
                                } // location added
                            case 0:
                                {
                                    System.out.println("Got visit " + (++InstallerServer.i) + " from " + user + ", " + designation);
                                    String reply = "Thank you. The sensor’s location was securely transmitted to GunshotSensing Inc.";
                                    byte[] msg = encryption.encrypt(reply.getBytes("US-ASCII"));
                                    out.write(msg);
                                    out.flush();
                                    InstallerServer.generate_kml();
                                    break;
                                } // authorization failure
                            case 5:
                                {
                                    System.out.println("Got visit " + (++InstallerServer.i) + " from " + user + ", " + designation + ", authorization failure to relocate sensor");
                                    String reply = "You dont have sufficient previleges to relocate the Sensor!";
                                    byte[] msg = encryption.encrypt(reply.getBytes("US-ASCII"));
                                    out.write(msg);
                                    out.flush();
                                    break;
                                } //authentication failure
                            default:
                                break;
                        }
                    } else { //Invalid credentials
                        System.out.println("Illegal password attempt, This may be an attack");
                        String reply = "Invalid ID or Password";
                        byte[] msg = encryption.encrypt(reply.getBytes("US-ASCII"));
                        out.write(msg);
                        out.flush();

                    }
                } else {
                    //if the decrypted message cannot be encoded in ASCII then 
                    //the client would have furnished an invalid key-terminate 
                    //the connection
                    System.out.println("Illegal symmetric key used, This may be an attack");
                    String reply = "Connection refused";
                    byte[] msg = encryption.encrypt(reply.getBytes("US-ASCII"));
                    out.write(msg);
                    out.flush();
                    clientSocket.close();
                }          

            //out.write(s_loc_det);
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } finally {
//            try {
//                clientSocket.close();
//            } catch (IOException e) {/*close failed*/
//            }
        }

    }

//-----------------------Decrypt the incoming message---------------------------    
    private boolean decrypt_inc_cipher() {
        try {
            byte[] input = null;
            TEA encryption = new TEA(key.getBytes());
            int size = clientSocket.getInputStream().available();
            input = new byte[size];
            //Read message from socket and decrypt the cipher to obtain the original
            //JSON model
            in.read(input);
            byte[] s_loc_det = encryption.decrypt(input);
            String text = new String(s_loc_det);
            // check if the symetric key entered is valid
            CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

            //First check if the symetric key is valid and proceed only if client
            //provided the appropriate symetric key- if text is ASCII
            if (!asciiEncoder.canEncode(text)) {
                return false;
            }
            JSONObject sen_loc_det = new JSONObject(new String(s_loc_det));

            sid = sen_loc_det.get("Sensor ID").toString();

            //Credentials
            JSONObject cred = sen_loc_det.getJSONObject("Credentials");
            user = cred.get("ID").toString().trim();
            pass = cred.get("password").toString().trim();

            //Latitude
            latitude = sen_loc_det.get("Latitude").toString().trim();

            //Longitude
            longitude = sen_loc_det.get("Longitude").toString().trim();

            //Altitude
            altitude = sen_loc_det.get("Altitude").toString().trim();
        } catch (IOException ex) {
            System.out.println("Parse:" + ex.getMessage());
        }
        return true;
    }

}
