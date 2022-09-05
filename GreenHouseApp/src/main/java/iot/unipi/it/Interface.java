package iot.unipi.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.paho.client.mqttv3.MqttException;


//import org.eclipse.californium.core.CoapClient;
//import org.eclipse.californium.core.CoapResponse;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class Interface {

    public boolean irrigationReq = false;
    public boolean set = false;
    public String status = null;
    static CoAPServer coapServer = new CoAPServer(5683);
    static public Map<String,Resource> registeredResources = new TreeMap<String,Resource>();
    
    public static void main(String[] args) {

        startServer();
        
        try {
        	ClientMqttHumidity mqttSubHum = new ClientMqttHumidity();
        }catch (MqttException me) {
            me.printStackTrace();
        }
     

        showMenu();

    }


    private static void startServer() {
        new Thread() {
            public void run() {
                coapServer.start();
            }
        }.start();
    }

    public static void showMenu() {
    	System.out.print("\nxxxxxxxxxxxxxxxxxxx GREEN HOUSE AUTO SYSTEM xxxxxxxxxxxxxxxxxxx\n"
                + "\nThis application serves to monitor the humidity and temperature of soil in a green house and regulate those values accordingly.\n"
                + "\nxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n\n");
    }
    
    public void MonitorHumidity() {
    	int hum = 0;
		
  	    try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  String connectionUrl = "jdbc:mysql://localhost:3306/greenhousesql";
		  String query = "SELECT * FROM mqtt ORDER BY time DESC LIMIT 1;";
		  try {
			  Connection conn = DriverManager.getConnection(connectionUrl,"root","admin");
			  Statement st = conn.createStatement();
			  ResultSet rs = st.executeQuery(query);
			  
			  if (rs.next()) {//get first result
				  hum = rs.getInt(3);
		        }
			  
			  System.out.println(hum);
			  
			  if(hum < 20) {
				  irrigationReq = true;
				  String code = actuatorActivation("irrigation-actuator");
				  regulateHumidity(code);
			  } 
			  irrigationReq = false;
			  set = false;
			  conn.close();
		  }catch(SQLException e){
			  e.printStackTrace();
		  }
		  
	}
    
    public void regulateHumidity(String code) {
    	
		if(!code.startsWith("2")) {	
			System.err.println("error: " + code);
			return;
		}
		
		int min = 20;
		int max = 40;
		int newhum = (int)Math.floor(Math.random()*(max-min+1)+min);
		set = true;
		
		  SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		  Date d = new Date();
		  String[] tokens = dateFormat.format(d).split(" ");
		  String date = tokens[0];
		  String time = tokens[1];
		  
		 
    	System.out.println("Irrigation has been activated... \nHumidity after regulation: " + newhum + "\n");
    	storeMqttData(time, date, newhum, irrigationReq);
    	try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	actuatorDeactivation("irrigation-actuator");
    
    }
    
    public void storeMqttData(String time, String date, int humidity, boolean req) {
	    	
	  	  if (req && !set) { status = "Required";
	  	  }else if (req && set) { status = "Regulated";
	  	  }else if (!req) { status = "Non-Required";}

          String connectionUrl = "jdbc:mysql://localhost:3306/greenhousesql";
    	  String query = "INSERT INTO mqtt (time, date, humidity, irrigation) VALUES ('"+time+"','"+date+"','"+humidity+"','"+status+"')";
    
		  try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		  } catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		  }
	  	  
	  	  try {
	  		  Connection conn = DriverManager.getConnection(connectionUrl,"root","admin");
	  		  PreparedStatement ps = conn.prepareStatement(query);
	  		  ps.executeUpdate();
	  		  conn.close();
	  	  }catch(SQLException e){
	  		  e.printStackTrace();
	  	  }
	  		  
	}
    
 	public String actuatorActivation(String name) {
		
		/* Resource discovery */

		CoapClient client = new CoapClient(registeredResources.get(name).getCoapURI());
		
		CoapResponse res = client.post("mode="+ "on", MediaTypeRegistry.TEXT_PLAIN);
		
		String code = res.getCode().toString();
		
		registeredResources.get(name).setActuatorState(true);
		
		return code;
			    	
	}
 	
 	public void actuatorDeactivation(String name) {
		
		/* Resource discovery */

		CoapClient client = new CoapClient(registeredResources.get(name).getCoapURI());
		
		CoapResponse res = client.post("mode="+ "off", MediaTypeRegistry.TEXT_PLAIN);
		
		String code = res.getCode().toString();
		
		registeredResources.get(name).setActuatorState(false);
		
		if(!code.startsWith("2")) {	
			System.err.println("error: " + code);
			return;
		}		    	
	}
 	
}