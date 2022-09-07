package iot.unipi.it;

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


public class Interface {

    public boolean irrigationReq = false;
    public boolean heatingReq = false;
    public boolean hum_set = false;
    public boolean temp_set = false;
    public String irrigation_status = null;
    public String heating_status = null;
    static CoAPServer coapServer = new CoAPServer(5683);
    static public Map<String,Resource> registeredResources = new TreeMap<String,Resource>();
    
    public static void main(String[] args) throws MqttException {

        startServer();
        
        try {
        	ClientMqttHumidity MqttHum = new ClientMqttHumidity();
        }catch (Error e) {
        	System.out.print("Here, don't know why");
        	System.out.print(e);
        }
        
        try {
        	ClientMqttTemperature MqttTemp = new ClientMqttTemperature();
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
                + "\nThis application serves to monitor the humidity and temperature of soil in a greenhouse and regulate those values accordingly.\n"
                + "\nxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n\n");
    }
    
    public void MonitorHumidity() throws SQLException {
		int hum = 0;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  String connectionUrl = "jdbc:mysql://localhost:3306/greenhousesql";
		  String query = "SELECT * FROM mqtt_humidity ORDER BY time DESC LIMIT 1;";
		  try {
			  Connection conn = DriverManager.getConnection(connectionUrl,"root","admin");
			  Statement st = conn.createStatement();
			  ResultSet rs = st.executeQuery(query);
			  
			  if (rs.next()) {//get first result
				  hum = rs.getInt(3);
		        }
			  
			  
			  if(hum < 20) {
				  System.out.println("Irrigation is required.\n");
				  irrigationReq = true;
				  actuatorActivation("irrigation-actuator");
				  regulateHumidity();
			  } else {System.out.println("Irrigation is not required.\n");}
			  
			  irrigationReq = false;
			  hum_set = false;
			  conn.close();
			  
		  }catch(SQLException e){
			  e.printStackTrace();
		  }  
	}
    
    public void regulateHumidity() {
		
		int min = 20;
		int max = 40;
		int newhum = (int)Math.floor(Math.random()*(max-min+1)+min);
		hum_set = true;
		irrigation_status = "ON";
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date d = new Date();
		String[] tokens = dateFormat.format(d).split(" ");
		String date = tokens[0];
		String time = tokens[1];
		  
    	System.out.println("Irrigation actuator: " + irrigation_status + "\n");
    	
    	storeMqttData(time, date, newhum, irrigationReq, "mqtt_humidity");
    	
    	try {	
			TimeUnit.SECONDS.sleep(5);
			
			System.out.println("Irrigation Complete!");
			actuatorDeactivation("irrigation-actuator");
			irrigation_status = "OFF";
			System.out.println("Irrigation actuator: " + irrigation_status);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	

    	System.out.println("Humidity after regulation: " + newhum + "\n");
    
    }
    
    public void MonitorTemperature() throws SQLException {
    	int temp = 0;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  String connectionUrl = "jdbc:mysql://localhost:3306/greenhousesql";
		  String query = "SELECT * FROM mqtt_temperature ORDER BY time DESC LIMIT 1;";
		  try {
			  Connection conn = DriverManager.getConnection(connectionUrl,"root","admin");
			  Statement st = conn.createStatement();
			  ResultSet rs = st.executeQuery(query);
			  
			  if (rs.next()) {//get first result
				  temp = rs.getInt(3);
		        }
			  
			  
			  if(temp < 10) {
				  System.out.println("Heating is required.\n");
				  heatingReq = true;
				  actuatorActivation("heating-actuator");
				  regulateTemperature();
			  } else {System.out.println("Heating is not required.\n");}
			  
			  heatingReq = false;
			  temp_set = false;
			  conn.close();
			  
		  }catch(SQLException e){
			  e.printStackTrace();
		  }  
	}
    
    public void regulateTemperature() {
		
		int min = 10;
		int max = 20;
		int newtemp = (int)Math.floor(Math.random()*(max-min+1)+min);
		temp_set = true;
		heating_status = "ON";
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date d = new Date();
		String[] tokens = dateFormat.format(d).split(" ");
		String date = tokens[0];
		String time = tokens[1];
		  
    	System.out.println("Heating actuator: " + heating_status + "\n");
    	
    	storeMqttData(time, date, newtemp, heatingReq, "mqtt_temperature");
    	
    	try {	
			TimeUnit.SECONDS.sleep(5);
			
			System.out.println("Heating Complete!");
			actuatorDeactivation("heating-actuator");
			heating_status = "OFF";
			System.out.println("Heating actuator: " + heating_status );
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	

    	System.out.println("Humidity after regulation: " + newtemp + "\n");
    
    }
    
    public void storeMqttData(String time, String date, int value, boolean req, String tableName) {
    	String query = null;
    	
	    if (tableName == "mqtt_humidity") {
	  	  if (req && !hum_set) { irrigation_status = "Required";
	  	  }else if (req && hum_set) { irrigation_status = "Regulated";
	  	  }else if (!req) { irrigation_status = "Non-Required";}
	  	  
	  	  query = "INSERT INTO "+ tableName +" (time, date, humidity, irrigation) VALUES ('"+time+"','"+date+"','"+value+"','"+irrigation_status+"')";
	    
	    }else if (tableName == "mqtt_temperature") {
	  	  if (req && !temp_set) { heating_status = "Required";
	  	  }else if (req && temp_set) { heating_status = "Regulated";
	  	  }else if (!req) { heating_status = "Non-Required";}
	  	  
		  query = "INSERT INTO "+ tableName +" (time, date, temperature, heating) VALUES ('"+time+"','"+date+"','"+value+"','"+heating_status+"')";
		}
	       	
		  try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		  } catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		  }
		  
		  String connectionUrl = "jdbc:mysql://localhost:3306/greenhousesql";
	  	  try {
	  		  Connection conn = DriverManager.getConnection(connectionUrl,"root","admin");
	  		  PreparedStatement ps = conn.prepareStatement(query);
	  		  ps.executeUpdate();
	  		  conn.close();
	  		  
	  	  }catch(SQLException e){
	  		  e.printStackTrace();
	  	  }
	  		  
	}
    
 	public void actuatorActivation(String name) {
		/* Resource discovery */

		CoapClient client = new CoapClient(registeredResources.get(name).getCoapURI());
		
		CoapResponse res = client.post("mode="+ "on", MediaTypeRegistry.TEXT_PLAIN);
		
		String code = res.getCode().toString();
		
		registeredResources.get(name).setActuatorState(true);
		
		if(!code.startsWith("2")) {	
			System.err.print("error: " + code);
			throw new Error ("Actuator Not Turned ON!!");
		}	
			    	
	}
 	
 	public void actuatorDeactivation(String name) {
		/* Resource discovery */

		CoapClient client = new CoapClient(registeredResources.get(name).getCoapURI());
		
		CoapResponse res = client.post("mode="+ "off", MediaTypeRegistry.TEXT_PLAIN);
	
		String code = res.getCode().toString();
		
		registeredResources.get(name).setActuatorState(false);
		
		if(!code.startsWith("2")) {	
			System.err.println("error: " + code);
			throw new Error ("Actuator Not Turned OFF!!");
		}		    	
	}
 	
}