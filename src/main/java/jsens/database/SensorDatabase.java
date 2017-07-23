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

package jsens.database;

import com.RDFHSalzburg.sensorrest.SensorNode;
import jsens.Preconf;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;

public class SensorDatabase {

    // Mysql database connection settings for meshlium
    // public static String StrUrl="jdbc:mysql://localhost:3306/SensDB";
    // public static String StrUid= "root";
    // public static String StrPwd= "libelium2007";
    // Mysql database connection settings local
    // public static String StrUrl="jdbc:mysql://localhost:3306/sensdb";
    // public static String StrUid= "sensadmin";
    // public static String StrPwd= "sensadmin";

    public static SensorDatabase instance = null;
    Connection conn = null;
    //    private final Lock lock = new ReentrantLock();
    private static final Logger logger =  LogManager.getLogger(SensorDatabase.class);

    private SensorDatabase() throws SQLException {
        // Load the database driver
//        Class.forName("com.mysql.cj.jdbc.Driver");
        this.open();
    }

    // Singleton
    public static synchronized SensorDatabase getinstance() throws SQLException {
        if (instance == null) {
            instance = new SensorDatabase();
        }
        return instance;
    }

    public static boolean isUnique(ResultSet rs) throws SQLException {
        rs.last();
        // int size = rs.getRow() * rs.getMetaData().getColumnCount();
        int size = rs.getRow();
        rs.beforeFirst();
        if (size != 1) {
            return false;
        }
        else {
            return true;
        }
    }

    public synchronized static int getSize(ResultSet rs) throws SQLException {
        if (rs == null) {
            return 0;
        }

        rs.last();
        int size = rs.getRow();
        rs.beforeFirst();
        return size;
    }

    public void open() throws SQLException {
        conn = DriverManager.getConnection(Preconf.databaseUrl, Preconf.databaseUser, Preconf.databasePassword);
        for (SQLWarning warn = conn.getWarnings(); warn != null; warn = warn.getNextWarning()) {
            logger.warn("SQL Warning:");
            logger.warn("State    : " + warn.getSQLState());
            logger.warn("Message  : " + warn.getMessage());
            logger.warn("ErrorCode: " + warn.getErrorCode());
        }

        //,ö    MainClass.getLogger().debug("Database opened!");
    }

    public void close() throws SQLException {
        conn.close();
    }

    public synchronized ResultSet getSensorNode(String ExtendedAddress) throws SQLException {
        ResultSet rs = null;
        String selectSt = "SELECT idSensorNode, ExtendedAddress, Name, Description, Latitude, Longitude, Altitude FROM SensorNodes WHERE ExtendedAddress = ?;";
        PreparedStatement ps = conn.prepareStatement(selectSt);
        ps.setString(1, ExtendedAddress);

        rs = ps.executeQuery();

        if (!SensorDatabase.isUnique(rs)) {
            return null;
        }
        else {
            rs.next();
        }

        return rs;
    }

    public synchronized ResultSet getSensorType(int SensID) throws SQLException {
        ResultSet rs = null;
        String selectSt = "SELECT idSensorType, SensID, SensorName, Placement, Phenomenon, Unit, Description FROM SensorTypes WHERE SensID = ?";
        PreparedStatement ps = conn.prepareStatement(selectSt);
        ps.setInt(1, SensID);
        rs = ps.executeQuery();

        if (!SensorDatabase.isUnique(rs)) {
            return null;
        }
        else {
            rs.next();
        }

        return rs;
    }

    public synchronized ResultSet getSensorMeasurements(String whereString) throws SQLException {
        String whereStr = "SELECT * FROM SensorMeasurements " + " WHERE " + whereString;
        Statement st = conn.createStatement();
        ResultSet rs;
        rs = st.executeQuery(whereStr);

//        if (rs == null) {
//            return null;
//        }
//        if (getSize(rs) == 0) {
//            return null;
//        }

        return rs;

    }

    public synchronized ResultSet getSensorMeasurementsWithFullInformation(String whereString) throws SQLException {
        //FIXME SREI SQL INJECTION!!!
        String whereStr = "SELECT * FROM SensorMeasurements, SensorNodes, SensorTypes " + "WHERE " + whereString;
        Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        ResultSet rs;
        logger.debug("Query: " + whereStr);
        rs = st.executeQuery(whereStr);

        if (rs == null) {
            return null;
        }
        if (getSize(rs) == 0) {
            return null;
        }

        return rs;
    }
    
    public synchronized SensorNode getSensorNode(int SensID) throws SQLException {
        //FIXME SREI SQL INJECTION!!!
        String query = "select * from sensornodes where idsensornode=?";

        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, SensID);
        ResultSet rs = ps.executeQuery();
        logger.debug("Query: " + query);
        SensorNode tempNode = null;
        
        while (rs.next()) {
            tempNode = new SensorNode(rs);
        }
        
        return tempNode;
    }
    
    

    public synchronized int updateSensorMeasurement(String setString, String whereString) throws SQLException {
        //FIXME SREI SQL INJECTION!!!
        String updateString = "UPDATE SensorMeasurements " + "SET " + setString + " WHERE " + whereString + ";";
        Statement st = conn.createStatement();

        return st.executeUpdate(updateString);
    }

    public synchronized int getSensorCount()  throws SQLException{
        String whereStr = "SELECT COUNT(*) AS rowcount FROM sensornodes";
        
        Statement st = conn.createStatement();
        ResultSet rs;
        logger.debug("Query: " + whereStr);
        rs = st.executeQuery(whereStr);
        
        rs.next();
        int count = rs.getInt("rowcount");
        rs.close();
        
        return count;
    }
    
    public synchronized int getSensorMeasurementsCount()  throws SQLException{
        String whereStr = "SELECT COUNT(*) AS rowcount FROM sensormeasurements";
        
        Statement st = conn.createStatement();
        ResultSet rs;
        logger.debug("Query: " + whereStr);
        rs = st.executeQuery(whereStr);
        
        rs.next();
        int count = rs.getInt("rowcount");
        rs.close();
        
        return count;
    }
    
    public synchronized int getSensorMeasurementsFalseCount()  throws SQLException{
        String whereStr = "SELECT COUNT(*) AS rowcount FROM sensormeasurements WHERE SOSTRANSMITTED = FALSE";
        
        Statement st = conn.createStatement();
        ResultSet rs;
        logger.debug("Query: " + whereStr);
        rs = st.executeQuery(whereStr);
        
        rs.next();
        int count = rs.getInt("rowcount");
        rs.close();
        
        return count;
    }
    
    public synchronized String getLastRecordDate() throws SQLException{
        String query = "SELECT * FROM SENSORMEASUREMENTS ORDER BY IDSENSORMEASUREMENT DESC LIMIT 1";
        
        Statement st = conn.createStatement();
        ResultSet rs;
        logger.debug("Query: " + query);
        rs = st.executeQuery(query);
        
        rs.next();
        String date = rs.getString("MEASTIME");
        
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime dateTime = LocalDateTime.from(f.parse(date));
        
        rs.close();
        return dateTime.toString();
        
        //return date.toInstant().toString();
    }
    
    public synchronized List<SensorNode> getAllSensors() throws SQLException {
        
        List<SensorNode> nodeList = new ArrayList<>();
        
        String query = "SELECT * FROM sensornodes";
        
        Statement st = conn.createStatement();
        ResultSet rs;
        logger.debug("Query: " + query);
        rs = st.executeQuery(query);
        
        while (rs.next()) {
            SensorNode tempNode = new SensorNode(rs);
            nodeList.add(tempNode);
        }
        
        return nodeList;
    }
    
    public synchronized int getActiveNodeCount(int timeoutMinutes) throws SQLException {
        String query = "SELECT COUNT(DISTINCT SENSORNODES_IDSENSORNODE) AS count FROM SENSORMEASUREMENTS WHERE MEASTIME > DATEADD(minute,?, NOW())";
        //"SELECT COUNT(*) AS rowcount FROM SENSORMEASUREMENTS WHERE SENSORNODES_IDSENSORNODE = 6 and MEASTIME > DATEADD(minute,30, NOW())";
        

        ResultSet rs;
        logger.debug("Query: " + query);
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, timeoutMinutes);

        rs = ps.executeQuery();
        
        rs.next();
        int count = rs.getInt("count");
        rs.close();
        
        return count;
    }
    
    public synchronized boolean insertSensorNode(String ExtendedAddress, String Name, String Description, float Latitude, float Longitude, float Altitude)
    throws SQLException {
        ResultSet rsSensorNode = null;
        ResultSet rsSensorType = null;
        PreparedStatement ps = null;
        
            String insertSt = "INSERT INTO sensornodes(ExtendedAddress, Name, Description, Latitude, Longitude, Altitude)"
                    + " VALUES( ?, ?, ?, ?, ?, ? )";

            ps = conn.prepareStatement(insertSt);
            ps.setString(1, ExtendedAddress);
            ps.setString(2, Name);
            ps.setString(3, Description);
            ps.setFloat(4, Latitude);
            ps.setFloat(5, Longitude);
            ps.setFloat(6, Altitude);

            String test = ps.toString();

            logger.debug(test);

            ps.execute();
            
            return true;
    }
    
    /**
     * Inserts SensorValue in data base Firstly SensorNode is fetched from table SensorNodes with String ExtendedAddress -> This result must be unique Secondly
     * SensorType is fetched from table SensorTypes with int SensID -> This result must be unique
     *
     * @param ExtendedAddress MAC address from Waspmote
     * @param SensID          Protocol sensor ID (e.g. 0x0020 for temp sensor 1, 0x0021 for temp sensor 2)
     * @param ts              Timestamp of measurement
     * @param RawValue        value which is transferred from Waspmote
     * @param CalcValue       value which was calculated for the sensor
     * @throws SQLException
     */
    public synchronized boolean insertSensorMeasurement(String ExtendedAddress, Integer SensID, Timestamp ts, Double RawValue, Double CalcValue)
            throws SQLException {
        ResultSet rsSensorNode = null;
        ResultSet rsSensorType = null;
        PreparedStatement ps = null;

        // String insertSt =
        // "INSERT INTO SensorMeasurements(MeasTime, Latitude, Longitude, Altitude, RawValue, CalcValue, SosTransmitted, SensorNodes_idSensorNode)"
        // + " VALUES(?, ?, ?, ?, ?, ?, ?, (SELECT idSensorNode from SensorNodes WHERE ExtendedAddress = ?))";

        try {

            rsSensorNode = this.getSensorNode(ExtendedAddress);
            rsSensorType = this.getSensorType(SensID);

            if (rsSensorNode == null) {
                logger.error(SensorDatabase.class.getName() + " Could not find Sensor Node: " + ExtendedAddress);
                return false;
            }
            if (!SensorDatabase.isUnique(rsSensorNode)) {
                logger.error(SensorDatabase.class.getName() + " SensorNode is not unique: " + ExtendedAddress);
                return false;
            }
            if (rsSensorType == null) {
                logger.error(SensorDatabase.class.getName() + " Could not find Sensor Type: " + String.valueOf(SensID));
                return false;
            }
            if (!SensorDatabase.isUnique(rsSensorType)) {
                logger.error(SensorDatabase.class.getName() + " SensorType is not unique: " + String.valueOf(SensID));
                return false;
            }

            rsSensorNode.next();
            rsSensorType.next();

            // Get values to insert SensorMeasurement
            int idSensorNode = rsSensorNode.getInt("idSensorNode");
            Double lat = rsSensorNode.getDouble("Latitude");
            Double lon = rsSensorNode.getDouble("Longitude");
            Double alt = rsSensorNode.getDouble("Altitude");

            int idSensorType = rsSensorType.getInt("idSensorType");

            String insertSt = "INSERT INTO SensorMeasurements(MeasTime, Latitude, Longitude, Altitude, RawValue, CalcValue, SosTransmitted, SosErrorCode, SensorNodes_idSensorNode, SensorTypes_idSensorType)"
                    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

            ps = conn.prepareStatement(insertSt);
            ps.setTimestamp(1, ts);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ps.setDouble(4, alt);
            ps.setDouble(5, RawValue);

            if (CalcValue == null) {
                ps.setObject(6, null);
            }
            else {
                ps.setDouble(6, CalcValue);
            }

            ps.setBoolean(7, false);
            ps.setInt(8, 0);
            ps.setInt(9, idSensorNode);
            ps.setInt(10, idSensorType);

            String test = ps.toString();

            logger.debug(test);

            ps.execute();
        }
        catch (SQLException e) {
            logger.error("SQL Exception in insertSensorMeasurement.", e);
            throw e;
        }
        finally {
            if (rsSensorNode != null) {
                rsSensorNode.close();
            }

            if (rsSensorType != null) {
                rsSensorType.close();
            }
            if (ps != null) {
                ps.close();
            }
        }

        return true;
    }

}
