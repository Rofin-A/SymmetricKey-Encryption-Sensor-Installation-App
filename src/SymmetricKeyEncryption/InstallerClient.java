/**
 * @classname: InstallerClient.Java
 * @author: Rofin A
 */
/*----------------------------------------------------------------------------- 
*  Purpose: This class serves as the client where the installers can log in and
trasmit details(location) regarding the sensor installation. The client requires
appropriate symmetric key and legitimate credentials for the server to accept 
the update request. In case of authentication or authorization failure, server
will not update the details sent and will return an error message 
will not update the details sent and will return an error message stating the 
reason for failure
-------------------------------------------------------------------------------*/
package SymmetricKeyEncryption;

/**
 *
 * @author rofin
 */
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Scanner;
import org.json.JSONObject;

public class InstallerClient {

//----Global Variable declaration
//--------------------------------
    private String key   = null;
    private String user  = null;
    private String pass  = null;
    private String sid   = null;
    private String coord = null;

    public static void main(String args[]) {
        // Object Instantiation
        InstallerClient installer = new InstallerClient();
        //Instantiae the socket for establishing communication between server 
        Socket s = null;
//--------------Establish connection with server and communicate----------------
        try {
           // while (true) {
//          Establish socket connection from client (localhost) to the 
//          port 7896 of the server
                int serverPort = 7896;
                s = new Socket("localhost", serverPort);
                DataInputStream in = new DataInputStream(s.getInputStream());
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
//          Read user input and set the gloabl varaible accordingly
                read_user_inp(installer);
                
//----------------Transform the installers input into a JSON model------------- 
                JSONObject s_info = build_json(installer);

//-------Encrypt the generated JSON model for secure transmission of data-------

//TEA algortithm is used for encryption
                TEA encryption = new TEA(installer.key.getBytes());
                byte[] s_loc_det = encryption.encrypt(s_info.toString().getBytes());
                out.write(s_loc_det); // Encrypted byte array is written to the channel
                out.flush();

                // initialize a byte array to read the incoming encrypted message 
                byte[] input = null;
                //wait until the data becomes available in the channel
                while (s.getInputStream().available() <= 0) {
                }
                // create buffer to read data from the socket
                int size = s.getInputStream().available();
                //initialize the byte array of the size determined abaove
                input = new byte[size];
                in.read(input);
                byte[] reply = encryption.decrypt(input);
                String msg = new String(reply);

//--------------check if the incoming data is ASCII compliant-------------------
// if not then the symetric key provided at client isnt valid-Exception
                CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

                //First check if the symetric key is valid and proceed only if client
                //provided the appropriate symetric key- if text is ASCII
                if (!asciiEncoder.canEncode(msg)) {
                    throw new ConnectionRefusedException("Connection refused by server");
                }

                System.out.println(msg.replaceAll("\\?", "\\'")); //"ascii encoding at server side turns quotes to question mark.reverting it
                
            //}
        } catch (UnknownHostException e) {
            System.out.println("Socket:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } catch (ConnectionRefusedException e){
            System.out.println("Fatal error:" + e.getMessage());
        } 
        finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
            }
        }
    }

    /*This method is for reading the user input and capture it into global
      variables and these variables will be used to create the JSON model
      for secure transmission*/
    private static void read_user_inp(InstallerClient installer) {
        //Console operation - Prompt user for details
        Scanner usr_inp = new Scanner(System.in);
        System.out.println("\nEnter symmetric key as a 16-digit integer :");
        installer.key = usr_inp.nextLine();

        System.out.println("Enter your ID:");
        installer.user = usr_inp.nextLine();

        System.out.println("Enter your Password:");
        installer.pass = usr_inp.nextLine();

        System.out.println("Enter sensor ID:");
        installer.sid = usr_inp.nextLine();

        System.out.println("Enter new sensor location:");
        installer.coord = usr_inp.nextLine();

    }

    /* This method is for generating the JSON model from user input */
    private static JSONObject build_json(InstallerClient installer) {
        JSONObject s_info = new JSONObject();

        // Populate the credential node in the json
        JSONObject cred = new JSONObject();
        cred.put("ID", installer.user);
        cred.put("password", installer.pass);

        s_info.put("Credentials", cred);
        //populate sensor id node 
        s_info.put("Sensor ID", installer.sid);
       
        //populate the coordinates
        if (installer.coord != null) {
            String[] coordinates = installer.coord.split(",");

            if (coordinates.length >= 1) //Populate the lattitude value 
            {
                s_info.put("Latitude", coordinates[0]);
            }
            else{s_info.put("Latitude", "0");}
            if (coordinates.length >= 2) //Populate the longitude value 
            {
                s_info.put("Longitude", coordinates[1]);
            }
            else {s_info.put("Longitude", "0");}
            if (coordinates.length == 3) //Populate the Altitude value 
            {
                s_info.put("Altitude", coordinates[2]);
            }
            else{s_info.put("Altitude", "0");}
        }
        return s_info;
    }
// Custom exception
    public static class ConnectionRefusedException extends RuntimeException {

        public ConnectionRefusedException(String message) {
            super(message);
        }
    }
}
