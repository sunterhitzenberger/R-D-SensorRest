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

package jsens.sos;


//import jsens.DataProcessor;
import jsens.database.SensorDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.apache.logging.log4j.LogManager;


public class SosConnectThread extends Thread {
    public enum ThreadResult{
        failed,
        successful,
        noUpdate
    };
    
    private ThreadResult result;
    private static final String NetworkID = "koppl";
    private static final org.apache.logging.log4j.Logger logger =  LogManager.getLogger(SosConnectThread.class);
    private static boolean takePCTime = false;
    private SensorDatabase db = null;

    public SosConnectThread(boolean takePCTime) throws SQLException {
        SosConnectThread.takePCTime = takePCTime;
        db = SensorDatabase.getinstance();

        // preliminary initial SosConnector setting
//        Logger SosLogger = org.apache.log4j.Logger.getLogger(SosConnector.class);
//        SosLogger.setLevel(Preconf.logLevelSos);
        SosConnector.HTTP_TIMEOUT_MS = 10000;
    }
    
    public ThreadResult getActualResult()
    {
        return this.result;
    }

    @Override
    public void run() {
//        try {
            int ret = 0;

            ret = processMeasurements();

            if (ret == 0) {
                logger.info("Nothing to update");
                result = ThreadResult.noUpdate;
            }
            else if (ret > 0) {
                logger.info("Succesfully updated " + ret + " entries!");
                result = ThreadResult.successful;
            }
            else {
                logger.info("Error while sos connection");
                result = ThreadResult.failed;
            }

            //DataProcessor.getinstance().setSosState(SosState.FINISHED);
//        }
//        catch (ClassNotFoundException ex) {
//            logger.error("Could not find Database driver.", ex);
//        }
//        catch (SQLException ex) {
//            logger.error("Error while SQL processing.", ex);
//        }
    }

    public int insertSingleMeasurement(String sensorID, String platformID, String networkID, String obsProp, String uomCode, Double val, Date phenTime) {
        int ret = 0;
        Double[] pos = {0.00, 0.00, 0.00};
        //Check if network exists
        ret = existsSensor(networkID);

        if (ret != 0) {
            //Network does not exist - try to insert network
            logger.debug("Sensor " + networkID + " does not exist.");
            ret = insertNetwork(networkID);
            if (ret != 0) {
                logger.warn("Could not insert Sensor " + networkID);
                return -1;
            }
        }

        ret = existsSensor(networkID + "/" + platformID);

        if (ret != 0) {
            //Platform does not exist - try to insert platform
            ret = SosConnector.insertPlatformSensor(platformID, networkID, obsProp, uomCode, pos);
            // TODO BatteryLevel hier anlegen - obProp is bat_level
            // TODO uomCode would be percent prefeably [%]
            // Source Code File has to be UTF8
        }
        if (ret != 0) {
            //Cant insert platformID
            return -2;
        }

        ret = existsSensor(networkID + "/" + platformID + "/" + sensorID);

        if (ret != 0) {
            ret = SosConnector.insertChildSensor(sensorID, platformID, networkID, obsProp, uomCode);
            //TODO here messwerte
        }

        if (ret != 0) {
            //Cant insert sensor
            return -3;
        }

        //At this postion setup must be done

        ret = SosConnector.insertChildSensorMeasurement(sensorID, platformID, networkID, obsProp, uomCode, val, phenTime);
        if (ret != 0) {
            logger.warn("Could not insert measurement.");
            //Cant insert measurement
            return -4;
        }
        return 0;
    }

    public int insertSingleMeasurementReverse(String sensorID, String platformID, String networkID, String obsProp, String uomCode, Double val, Date phenTime) {
        int ret = 0;
        Double[] pos = {0.00, 0.00, 0.00};

        ret = SosConnector.insertChildSensorMeasurement(sensorID, platformID, networkID, obsProp, uomCode, val, phenTime);
        if (ret == 0) {
            //Measurement inserted
            return 0;
        }

        //Cant insert, check if setup is OK

        //Check if network exists
        logger.debug("Checking Network existence: " + networkID);
        ret = existsSensor(networkID);

        if (ret != 0) {
            //Network does not exist - try to insert network
            ret = insertNetwork(networkID);
        }
        if (ret != 0) {
            //cant insert networkID
            return -1;
        }

        logger.debug("Checking Platform existence: " + networkID + "/" + platformID);
        ret = existsSensor(networkID + "/" + platformID);

        if (ret != 0) {
            //Platform does not exist - try to insert platform
            ret = SosConnector.insertPlatformSensor(platformID, networkID, obsProp, uomCode, pos);
            // Todo BatteryLevel hier anlegen - obProp is bat_level
            // Todo uomCode would be percent prefeably [%]
            // Source Code File has to be UTF8
        }
        if (ret != 0) {
            //cant insert platformID
            return -2;
        }

        logger.debug("Checking Sensor existence: " + networkID + "/" + platformID + "/" + sensorID);
        ret = existsSensor(networkID + "/" + platformID + "/" + sensorID);

        if (ret != 0) {
            ret = SosConnector.insertChildSensor(sensorID, platformID, networkID, obsProp, uomCode);
            //todo here messwerte
        }

        if (ret != 0) {
            //cant insert sensor
            return -3;
        }

        //try again to insert Measurement

        ret = SosConnector.insertChildSensorMeasurement(sensorID, platformID, networkID, obsProp, uomCode, val, phenTime);
        return ret;
    }

    public int insertSingleMeasurementFast(String sensorID, String platformID, String networkID, String obsProp, String uomCode, Double val, Date phenTime) {
        int ret = 0;
        Double[] pos = {0.00, 0.00, 0.00};

        ret = SosConnector.insertChildSensorMeasurement(sensorID, platformID, networkID, obsProp, uomCode, val, phenTime);
        if (ret != 0) {
            //cant insert measurement
            return -4;
        }
        return 0;
    }

    public int processMeasurements() {
        int measurementCount;
        try {
            logger.info("Querying for untransmitted measurements...");

            String whereString = "SensorMeasurements.SosTransmitted = FALSE"
                    +" AND SensorMeasurements.SosErrorCode = 0"
                    +" AND SensorMeasurements.SensorNodes_idSensorNode = SensorNodes.idSensorNode"
                    +" AND SensorMeasurements.SensorTypes_idSensorType = SensorTypes.idSensorType"
                    +" ORDER BY idSensorMeasurement DESC limit 50"
                    ;

            ResultSet rs = db.getSensorMeasurementsWithFullInformation(whereString);

            measurementCount = SensorDatabase.getSize(rs);
            logger.info("Selected " + measurementCount + " untransmitted measurements.");

            if (measurementCount == 0) {
                return 0;
            }

            String sensorID;
            String platformID;
            String networkID = SosConnectThread.NetworkID;
            String obsProp;
            String uomCode;
            Double val;
            Integer idSensorMeasurement = null;

            java.sql.Timestamp ts = null;
            Date phenTime = null;

            //If there is one sosupdated entry for sensorid platformid and networkid sensor must exist

            int a = -55;

            while (rs.next())      //process resultset
            {
                platformID = /*rs.getString("ExtendedAddress") + "_" +*/rs.getString("Name").toLowerCase();
                obsProp = rs.getString("Phenomenon").replaceAll(" ", "_").toLowerCase();
                sensorID = ("p" + rs.getString("SensID") + "_" + obsProp).toLowerCase();
                uomCode = rs.getString("Unit").toLowerCase();
                val = rs.getDouble("CalcValue");
                ts = rs.getTimestamp("MeasTime");
                idSensorMeasurement = rs.getInt("idSensorMeasurement");

                if (takePCTime == false) {
                    phenTime = new Date(ts.getTime());
                }
                else {
                    phenTime = new Date();
                }

                a = insertSingleMeasurementReverse(sensorID, platformID, networkID, obsProp, uomCode, val, phenTime);

                if (a == 0) {
                    db.updateSensorMeasurement("MeasTime=MeasTime, SosTransmitted=TRUE", "idSensorMeasurement = " + idSensorMeasurement);
                }
                else {
                    db.updateSensorMeasurement("MeasTime=MeasTime, SosTransmitted=FALSE, SosErrorCode=" + a, "idSensorMeasurement = " + idSensorMeasurement);
                }

            }
        }
        catch (SQLException ex) {
            logger.error("Error while updating measurement during SOS transfer. ", ex);
            return 1;
        }
        return measurementCount;
    }

    public int existsSensor(String PlatformId, String SensorID) {
        String sensorUri = SosConnector.URI_PREFIX + "/procedure/" + NetworkID + "/" + PlatformId + "/" + SensorID;
        logger.debug("existsSensor: " + sensorUri);
        int t = 0;
        t = SosConnector.existsSensor(sensorUri);
        return t;
    }

    public int existsSensor(String uri) {
        String sensorUri = SosConnector.URI_PREFIX + "/procedure/" + uri;
        logger.debug("existsSensor: " + sensorUri);

        int t = -1;
        t = SosConnector.existsSensor(sensorUri);
        return t;
    }

    public int insertNetwork(String networkID) {
        int ret = -1;
        ret = SosConnector.insertNetworkSensor(networkID);
        return ret;
    }

}
