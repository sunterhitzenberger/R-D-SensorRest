/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.sensorrest;
import com.google.gson.Gson;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import jsens.database.SensorDatabase;
import jsens.sos.SosConnectThread;
import jsens.sos.SosConnectThread.ThreadResult;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import static spark.Spark.*;
/**
 *
 * @author Admin
 */

public class MainClass {
    static int cpuUsagePrev = -1;
    static int memoryUsagePrev = -1;
    private static final Logger logger =  LogManager.getLogger(MainClass.class);   
    private static SosConnectThread connectThread;

    private static void initTestSetup(String workingDirPath){
        //File RestartDB = new File (workingDirPath);
        File startProgramms = new File(workingDirPath);
        startProgramms.e
    }
    
    public static void main(String[] args) {
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        File file = new File("log4j2.xml");
                        
        // this will force a reconfiguration
        context.setConfigLocation(file.toURI());        
        Gson gson = new Gson();
        
        // TODO: remove this
        // start H2 DB and GUI for Tests
        initTestSetup(file.toURI().toString());
        
        List<SensorNode> nodeList = new ArrayList<>();
        nodeList.add(new SensorNode("Hello World", "aaa"));
        nodeList.add(new SensorNode("Hello World", "55"));
        nodeList.add(new SensorNode("Hello World", "44"));

        //startSosThread();

        //get("/hello", (request, response) -> new SensorNode("Hello World", "aaa"), gson::toJson);
        get("/nodelist", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            SensorDatabase base = getDB();
            if(base != null)
                return base.getAllSensors();
            
            return null;
        }, gson::toJson);
        
        /**/
        
        get("/summary", (request, response) -> {
            //IP-Adress
            InetAddress IP=InetAddress.getLocalHost();
            
            SensorDatabase base = getDB();
            if(base == null)
                return "";
            
            int nodeCount = base.getSensorCount();
            int measurementCount = base.getSensorMeasurementsCount();
            
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            int mb = 1024*1024;
            
            //CPU
            if(cpuUsagePrev < 0)
                cpuUsagePrev = (int) (osBean.getSystemCpuLoad() * 100);
            int cpuUsage = (int) (osBean.getSystemCpuLoad() * 100);
            int cpuUsagePrevOld = cpuUsagePrev;
            cpuUsagePrev = cpuUsage;
            
            //Memory
            if(memoryUsagePrev < 0)
                memoryUsagePrev = (int)osBean.getFreePhysicalMemorySize() / mb;
            int memoryUsage = (int)osBean.getFreePhysicalMemorySize() / mb ;
            int memoryUsageOld = memoryUsagePrev;
            memoryUsagePrev = memoryUsageOld;
            
            response.header("Access-Control-Allow-Origin", "*");
            return "{\n" + "\"system\":{\n" +
"		\"memory\":{ \"displayName\":\"Memory\", \"used\":" + memoryUsage + ", \"available\":" + osBean.getTotalPhysicalMemorySize() / mb + ", \"unit\":\"MB\", \"previousUsed\": " + memoryUsageOld + "  },\n" +
"		\"cpu\":{ \"displayName\":\"CPU Usage\",\"used\": " + cpuUsage + ", \"unit\":\"%\", \"previousUsed\": " + cpuUsagePrevOld + " },\n" +
"		\"storage\":{ \"displayName\":\"Storage\", \"used\":120, \"available\":128, \"unit\":\"GB\"},\n" +
"		\"internet\":{ \"displayName\":\"Internet\",\"active\": " + InternetAvailabilityChecker.isInternetAvailable() + ", \"ip\": \"" + IP.getHostAddress() + "\", \"sos_reachable\": true, \"sos_ping\": 110}\n" +
"	},\n" +
"	\"nodes\":{ \"displayName\": \"Nodes\", \"registered\": " + nodeCount + ", \"active\": " + nodeCount + ", \"count\": " + nodeCount + "},\n" +
"	\"measurements\": { \"displayName\": \"Measurements\", \"sent\": " + measurementCount + ", \"not_sent\": " + measurementCount + ", \"last_upload\": \"2015-11-13T12:34:12.2Z\"},\n" +
"	\"webcam\":{\"displayName\": \"WebCam\", \"link\":\"http://www.reactionface.info/sites/default/files/images/1287666826226.png\"}\n" +
"}"; 
        });
        
        options("/nodedetails", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            return "";
        });
        
        get("/nodedetails", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            return "{\n" +
"	\"id\": 5, \n" +
"	\"address\": \"0013A2004061645D\", \n" +
"	\"name\": \"Agri01\", \n" +
"	\"description\": \"Agriculture Board on Waspmote 1.1\",\n" +
"	\"latitude\": 47.817471, \n" +
"	\"longitude\": 13.15454, \n" +
"	\"altitude\": 724.0,\n" +
"	\"version\": \"1.09b\",\n" +
"	\"battery\":\n" +
"	{\n" +
"		\"voltage\": \"10.8V\",\n" +
"		\"percent\": 77,\n" +
"		\"condition\": \"good\"\n" +
"	},\n" +
"	\"measurements\": [282,98,34]\n" +
"}";
        });
        
        /*Access-Control-Allow-Headers: Content-Type
        Access-Control-Allow-Methods: GET, POST, OPTIONS*/
        
        options("/node/create", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            return "";
        });
        
        put("/node/create", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            String body = request.body();
            //remove useless payload
            body = body.replace("{\"toCreate\":", "").replace("}}", "}");
            
            Node node = gson.fromJson(body, Node.class);
            
            //(int idSensorNode, String ExtendedAddress, String Name, String Description, float Latitude, float Longitude, float Altitude)
            SensorDatabase base = getDB();
            if(base != null)
                base.insertSensorNode(node.getAddress(), node.getName(), node.getDescription(), node.getLatitude(), node.getLongitude(), node.getAltitude());
            
            return "";
            // Create something
        });
        
        get("/log", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            return getLog(context);
        });
        
        get("/updateLog", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            return getLog(context);
        });
        
        get("/sendUnsend", (request, response) -> {
            startSosThread();
            response.header("Access-Control-Allow-Origin", "*");
            return "";
        });
        
        get("/redeemUnsuccessful", (request, response) -> {
            startSosThread();
            response.header("Access-Control-Allow-Origin", "*");
            return "";
        });
        
        get("/null", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            return "empty";
        });
        
        get("/restartGateway", (request, response) -> {
            
            response.header("Access-Control-Allow-Origin", "*");
            return "empty";
        });
    }
    
    private static String getLog(LoggerContext context){
        String logFileName = ((FileAppender)context.getConfiguration().getAppender("backendWrapper")).getFileName();
            File logFile = new File(logFileName);
            List<String> logLineList = new ArrayList<>();
            int limit = 500;
            int i = 0;
            String line;
            
            try(ReversedLinesFileReader reverseReader = new ReversedLinesFileReader(logFile)){
            // read all lines from bottom to top, until there is a blank line, file is finished or limit exeeded
                while ((line = reverseReader.readLine()) != null && i++ < limit)
                {
                    logLineList.add(line);
                }
            }
            catch(IOException ioe)
            {
                logger.error(ioe.getStackTrace());
            }
            
            // revert list to get the original line direction
            Collections.reverse(logLineList);
            
            // copy entries into a stringbuilder to simplier send a string back to the client.
            StringBuilder logLineBuilder = new StringBuilder();
            for (String tempLine : logLineList){
                logLineBuilder.append(tempLine + "\n");
            }
            
            return logLineBuilder.toString();
    }
        
    private static String startSosThread() {
        try {
            if (connectThread ==  null || connectThread.getState()==Thread.State.TERMINATED){
                logger.info("initialize sosConnectThread");
                connectThread = new SosConnectThread(false);
                connectThread.setName("Sos Thread");
            }
            if (connectThread.getState() == Thread.State.NEW || connectThread.getState() == Thread.State.WAITING)
            {
                logger.info("Starting SOS thread");
                connectThread.start();
                return "upload started";
            }
            else{
                logger.error("sosConnectThread is not in a startable state");
                return "upload not started";
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
        }   
        return "";
    } 
    
    public static SensorDatabase getDB(){
            SensorDatabase base = null;
            try {
                base = SensorDatabase.getinstance();
            } catch (SQLException ex) {
                //Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
            }
            return base;
    }
    
        private static class Node
        {
            private String address;

            private float altitude;

            private String description;

            private String name;

            private float longitude;

            private float latitude;

            public String getAddress ()
            {
                return address;
            }

            public void setAddress (String address)
            {
                this.address = address;
            }

            public float getAltitude ()
            {
                return altitude;
            }

            public void setAltitude (float altitude)
            {
                this.altitude = altitude;
            }

            public String getDescription ()
            {
                return description;
            }

            public void setDescription (String description)
            {
                this.description = description;
            }

            public String getName ()
            {
                return name;
            }

            public void setName (String name)
            {
                this.name = name;
            }

            public float getLongitude ()
            {
                return longitude;
            }

            public void setLongitude (float longitude)
            {
                this.longitude = longitude;
            }

            public float getLatitude ()
            {
                return latitude;
            }

            public void setLatitude (float latitude)
            {
                this.latitude = latitude;
            }

            @Override
            public String toString()
            {
                return "ClassPojo [address = "+address+", altitude = "+altitude+", description = "+description+", name = "+name+", longitude = "+longitude+", latitude = "+latitude+"]";
            }
        }
    
    private static class MyMessage {

        private String world;

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }
        
        public MyMessage(String hello_World) {
            this.world = hello_World;
        }
    }
}
