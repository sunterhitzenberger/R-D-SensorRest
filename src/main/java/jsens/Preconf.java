/*******************************************************************************
 * Copyright 2015 Z_GIS (www.zgis.at)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package jsens;

//import jsens.comm.WaspComm;
import jsens.database.SensorDatabase;
//import jsens.sos.SosConnectThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.CodeSource;
import java.util.Properties;
import org.apache.logging.log4j.Level;

//import static jsens.MainClass.Version_Number;

//TODO fix all sys.error.printline and sys.out.printline
public class Preconf {
    public static String databaseUrl = "jdbc:h2:tcp://localhost:9092/gateway2db/gateway2db;MODE=PostgreSQL;DATABASE_TO_UPPER=false";//"jdbc:mysql://localhost:3306/sensdb";
    public static String databaseUser = "";
    public static String databasePassword = "";

    public static String serialPort = "COM4";
    public static boolean useSos = false;
    public static boolean useSpa = true;

    public static String spaIP = "localhost";
    public static int spaPort = 4001;
    //TODO SREI fix this. Log config is in log4j properties and nowhere else
    public static org.apache.logging.log4j.Level logLevel;
    public static org.apache.logging.log4j.Level logLevelSos;

    public static void Init() {
        Properties prop = new Properties();

        try {
//        File f = new File(System.getProperty("java.class.path"));
//        File dir = f.getAbsoluteFile().getParentFile();
//        String path = dir.toString();
//        
//        System.out.println(path);

            CodeSource codeSource = Preconf.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            File jarDir = jarFile.getParentFile();

            if (jarDir != null && jarDir.isDirectory()) {
                File propFile = new File(jarDir, "sensorobservation.properties");
                FileInputStream fis = new FileInputStream(propFile);
                prop.load(fis);
                fis.close();
            }

            //prop.load(Preconf.class.getClassLoader().getResourceAsStream("sensorobservation.properties"));

            Preconf.databaseUrl = prop.getProperty("DataBaseUrl");
            Preconf.databaseUser = prop.getProperty("DataBaseUser");
            Preconf.databasePassword = prop.getProperty("DataBasePassword");
            Preconf.serialPort = prop.getProperty("ComPort");

            String s = prop.getProperty("UseSos");

            if (s.compareTo("TRUE") == 0) {
                Preconf.useSos = true;
            }
            else if (s.compareTo("FALSE") == 0) {
                Preconf.useSos = false;
            }
            else {
                System.err.println("useSos wrong config");
                System.exit(-1);
            }
            s = prop.getProperty("UseSpa");
            if (s.compareTo("TRUE") == 0) {
                Preconf.useSpa = true;
            }
            else if (s.compareTo("FALSE") == 0) {
                Preconf.useSpa = false;
            }
            else {
                System.err.println("useSpa wrong config");
                System.exit(-1);
            }

            Preconf.spaIP = prop.getProperty("SpaIP");
            s = prop.getProperty("SpaPort");
            Preconf.spaPort = Integer.valueOf(s);

            //FIXME delete this. Logconfig only via log4j.properties and class/packelevel
            s = prop.getProperty("LogLevel");

            if (s.toLowerCase().contains("debug") == true) {
                Preconf.logLevel = Level.DEBUG;
            }
            else if (s.toLowerCase().contains("all") == true) {
                Preconf.logLevel = Level.ALL;
            }
            else if (s.toLowerCase().contains("error") == true) {
                Preconf.logLevel = Level.ERROR;
            }
            else if (s.toLowerCase().contains("off") == true) {
                Preconf.logLevel = Level.OFF;
            }
            else if (s.toLowerCase().contains("info") == true) {
                Preconf.logLevel = Level.INFO;
            }
            else if (s.toLowerCase().contains("fatal") == true) {
                Preconf.logLevel = Level.FATAL;
            }
            else if (s.toLowerCase().contains("warn") == true) {
                Preconf.logLevel = Level.WARN;
            }
            else {
                System.err.println("logLevel wrong config");
                System.exit(-1);
            }

            //FIXME delete this. Logconfig only via log4j.properties and class/packelevel
            s = prop.getProperty("LogLevelSos");

            if (s.toLowerCase().contains("debug") == true) {
                Preconf.logLevelSos = Level.DEBUG;
            }
            else if (s.toLowerCase().contains("all") == true) {
                Preconf.logLevelSos = Level.ALL;
            }
            else if (s.toLowerCase().contains("error") == true) {
                Preconf.logLevelSos = Level.ERROR;
            }
            else if (s.toLowerCase().contains("off") == true) {
                Preconf.logLevelSos = Level.OFF;
            }
            else if (s.toLowerCase().contains("info") == true) {
                Preconf.logLevelSos = Level.INFO;
            }
            else if (s.toLowerCase().contains("fatal") == true) {
                Preconf.logLevelSos = Level.FATAL;
            }
            else if (s.toLowerCase().contains("warn") == true) {
                Preconf.logLevelSos = Level.WARN;
            }
            else {
                System.err.println("logLevelSos wrong config");
                System.exit(-1);
            }

            //FIXME SREI this is done in main class and ONLY in main class
            /*
            File fi = new File(jarDir + "\\log4j.properties");
            if (fi.exists() == true) {
                PropertyConfigurator.configure(jarDir + "\\log4j.properties");
            }
            else {
                fi = new File(jarDir + "/log4j.properties");
                if (fi.exists() == true) {
                    PropertyConfigurator.configure(jarDir + "/log4j.properties");
                }
            }
            */

            //Get the property value and print it
            System.out.println("databaseUrl:" + databaseUrl);
            System.out.println("databaseUser:" + databaseUser);
            System.out.println("databasePassword:" + databasePassword);
            System.out.println("serialPort:" + serialPort);
            System.out.println("spaIP:" + spaIP);
            System.out.println("spaPort:" + spaPort);
            System.out.println("useSos:" + String.valueOf(useSos));
            System.out.println("useSpa:" + String.valueOf(useSpa));
            System.out.println("logLevel:" + String.valueOf(logLevel.toString()));
            System.out.println("logLevelSos:" + String.valueOf(logLevelSos.toString()));

        }
        catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
    }

}
