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

import jsens.sos.models.ObservationDescription;
import jsens.sos.models.SOSConstants;
import jsens.sos.models.SOSConstants.entityType;
import jsens.sos.models.SensorDescription;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;


/**
 * works with uniqueSensorID URI_PREFIX + /procedure + /network + /platform +
 * /sensor http://vocab.smart-project.info/sensorweb/procedure + /baseID1 +
 * /waspID01 + /ID55
 */

public class SosConnector {
    private static final org.apache.logging.log4j.Logger logger =  LogManager.getLogger(SosConnector.class);

    public static int HTTP_TIMEOUT_MS = 10000;

    public static Level LOG_LEVEL = Level.INFO;

    //FIXME SREI PUT THIS IN properties file!
    public static String sos_url_kvp = "http://141.201.140.86:9090/sos/service/kvp?";

    public static String sos_url_pox = "http://141.201.140.86:9090/sos/service/pox";

    public static String URI_PREFIX = "http://vocab.smart-project.info/sensorweb";

    /**
     * does a DescribeSensor through SOS KVP GET interface works with
     * uniqueSensorID URI_PREFIX + /procedure + /network + /platform + /sensor
     * http://vocab.smart-project.info/sensorweb/procedure + /baseID1 +
     * /waspID01 + /ID55
     *
     * @param sensorURI
     * @return 0 - YES/exists, 1 - NO(actually invalidparam response), 4 - HTTP
     * ERROR, 5 - GENERIC OWS LOGIC ERRROR (should not really happen)
     * <p/>
     * does a DescribeSensor through SOS KVP GET interface
     */
    public static int existsSensor(String sensorURI) {
        logger.debug("existsSensor:" + sensorURI);

        StringBuilder kvpRequestParams = new StringBuilder();
        kvpRequestParams.append("service=" + "SOS");
        kvpRequestParams.append("&version=" + "2.0.0");
        kvpRequestParams.append("&request=" + "DescribeSensor");
        kvpRequestParams.append("&procedure=" + urlEncode(sensorURI));
        kvpRequestParams.append("&procedureDescriptionFormat="
                + urlEncode("http://www.opengis.net/sensorML/1.0.1"));

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpGet httpget = new HttpGet(sos_url_kvp + kvpRequestParams.toString());

        String responseBody = "";
        HttpResponse response;

        logger.debug("existsSensor: executing request: " + httpget.getURI());
        int returnCode = 5;

        try {
            // execute and parse the response

            response = httpclient.execute(httpget);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent(), "UTF-8"));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }

            logger.debug("existsSensor - Response:\n"
                            + "----------------------------------------\n"
                            + responseBody + "\n"
                            + "----------------------------------------"
            );

            if (responseBody.contains("ows:Exception")) {
                if (responseBody.contains("InvalidParameterValue")) {
                    logger.warn("existsSensor: InvalidParameterValue - sensorID does not exist");
                    returnCode = 1;
                }
                else {
                    logger.error("existsSensor: some other ows:Exception");
                    returnCode = 5;
                }
            }
            else {
                if (responseBody.contains("swes:DescribeSensorResponse")) {
                    if (responseBody.contains(sensorURI)) {
                        logger.debug("existsSensor: very likely to exist");
                        returnCode = 0;
                    }
                    else {
                        logger.error("existsSensor: unexpected error, swes response does not contain sensor ID");
                        returnCode = 5;
                    }
                }
                else {
                    logger.error("existsSensor: dont't know what to write, unlikely to come along here?");
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.error("existsSensor: error executing http get request - IOException", e);
            returnCode = 4;
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return returnCode;
    }

    /**
     * takes a uniqueSensorURI, assume independent group or
     * netowk/platform/sensor does a DeleteSensor through SOS KVP GET interface
     * creates default demogroup sensorURI
     *
     * @param sensorURI
     * @return 0 - deleted, 2 - InvalidParam/didn't exist, 4 - HTTP ERROR, 5 -
     * GENERIC OWS LOGIC ERRROR (should not really happen)
     */
    public static int deleteSensor(String sensorURI) {

        logger.info("deleteSensor: " + sensorURI);

        StringBuilder kvpRequestParams = new StringBuilder();
        kvpRequestParams.append("service=" + "SOS");
        kvpRequestParams.append("&version=" + "2.0.0");
        kvpRequestParams.append("&request=" + "DeleteSensor");
        kvpRequestParams.append("&procedure=" + urlEncode(sensorURI));

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpGet httpget = new HttpGet(sos_url_kvp + kvpRequestParams.toString());

        String responseBody = "";
        HttpResponse response;
        logger.debug("deleteSensor: executing request: " + httpget.getURI());
        int returnCode = 5;

        try {
            // execute and parse the response
            response = httpclient.execute(httpget);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent(), "UTF-8"));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }
            logger.debug("deleteSensor: ");
            logger.debug("----------------------------------------");
            logger.debug(responseBody);
            logger.debug("----------------------------------------");

            if (responseBody.contains("ows:Exception")) {

                if (responseBody.contains("InvalidParameterValue")) {
                    logger.info("deleteSensor: InvalidParameterValue - sensorID "
                            + sensorURI
                            + " does not exist, but therefore is kind of deleted");
                    returnCode = 2;
                }
                else {
                    logger.info("deleteSensor: some other ows:Exception");
                    logger.debug(responseBody);
                    returnCode = 5;
                }
            }
            else {
                if (responseBody.contains("swes:deletedProcedure")) {

                    if (responseBody.contains(sensorURI)) {
                        logger.info("deleteSensor: sensorID " + sensorURI
                                + " deleted");
                        returnCode = 0;
                    }
                    else {
                        logger.error("deleteSensor: unexpected error, swes response does not contain sensor ID");
                        logger.debug(responseBody);
                        returnCode = 5;
                    }
                }
                else {
                    logger.error("deleteSensor: dont't know what to write, unlikely to come along here?");
                    logger.debug(responseBody);
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.debug("deleteSensor: error executing http get request - IOException");
            e.printStackTrace();
            returnCode = 4;

        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return returnCode;
    }

    /**
     * insertNetworkSensor minimum for NETWORK sensorml per default
     *
     * @param networkID
     * @return
     */
    @Deprecated
    public static int insertNetworkSensor(String networkID) {
        SensorDescription mySml = new SensorDescription(networkID);
        entityType sensorType = SOSConstants.entityType.NETWORK;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID;
        //FIXME SREI sensors without observable properties are not valid
        return 0;//insertSensor(sensorType, networkID, mySml, sensorURI, "");
    }

    /**
     * insertNetworkSensor for NETWORK sensorml per default with status phenomenon, can also be empty obsProp and obsType!
     *
     * @param networkID
     * @param obsProp
     * @param uomCode
     * @param obsType   you can use MEASURMENT, TRUTH, CATEGORY, TEXT, COUNT
     * @param codeSpace (only here categoryCodeSpace for a codelist indicator)
     * @return
     */
    public static int insertNetworkSensor(String networkID, String obsProp, String uomCode, String obsType, String codeSpace) {
        SensorDescription mySml = new SensorDescription(networkID, obsProp, uomCode, obsType, codeSpace, true);
        entityType sensorType = SOSConstants.entityType.NETWORK;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID;
        String phenomenonURI = null;
        if ((obsProp != null) && !(obsProp.isEmpty())) {
            phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;
        }
        //FIXME SREI sensors without observable properties are not valid
        return 0;//insertSensor(sensorType, networkID, mySml, sensorURI, phenomenonURI, obsType);
    }

    /**
     * insertPlatformSensor with a default measurement obsProp, vague, deprecated
     *
     * @param platformID
     * @param networkID
     * @param observedProperty
     * @param uomCode
     * @param position
     * @return
     */
    @Deprecated
    public static int insertPlatformSensor(String platformID, String networkID,
                                           String observedProperty, String uomCode, Double[] position) {
        SensorDescription mySml = new SensorDescription(platformID, networkID,
                observedProperty, uomCode, position);
        entityType sensorType = SOSConstants.entityType.PLATFORM;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/"
                + platformID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + observedProperty;
        return insertSensor(sensorType, platformID, mySml, sensorURI,
                phenomenonURI);
    }

    /**
     * insertPlatformSensor for PLATFORM sensorml with status phenomenon, can also be empty observedProperty and obsType!
     *
     * @param platformID
     * @param networkID
     * @param observedProperty
     * @param uomCode
     * @param position
     * @param obsType          MEASUREMENT, COUNT, TEXT, CATEGORY, TRUTH
     * @param codeSpace        (will be only used for category as of now 0.0.7-SNAPSHOT)
     * @return
     */
    public static int insertPlatformSensor(String platformID, String networkID,
                                           String observedProperty, String uomCode, Double[] position, String obsType, String codeSpace) {
        SensorDescription mySml = new SensorDescription(platformID, networkID,
                observedProperty, uomCode, position, obsType, codeSpace);
        entityType sensorType = SOSConstants.entityType.PLATFORM;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/" + platformID;
        String phenomenonURI = null;
        if ((observedProperty != null) && !(observedProperty.isEmpty())) {
            phenomenonURI = URI_PREFIX + "/phenomenon/" + observedProperty;
        }
        return insertSensor(sensorType, platformID, mySml, sensorURI,
                phenomenonURI, obsType);
    }

    /**
     * insertChildSensor, only supports MEASUREMENT TYPE observation
     *
     * @param sensorID
     * @param platformID
     * @param networkID
     * @param observedProperty
     * @param uomCode
     * @return
     */
    @Deprecated
    public static int insertChildSensor(String sensorID, String platformID,
                                        String networkID, String observedProperty, String uomCode) {
        logger.debug("insertChildSensor: \n"
                        + "sensorID: " + sensorID + "\n"
                        + "platformID:" + platformID + "\n"
                        + "networkID: " + networkID + "\n"
                        + "observedProperty: " + observedProperty
        );
        SensorDescription mySml = new SensorDescription(sensorID, platformID,
                networkID, observedProperty, uomCode);
        entityType sensorType = SOSConstants.entityType.SENSOR;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/"
                + platformID + "/" + sensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + observedProperty;
        return insertSensor(sensorType, platformID, mySml, sensorURI,
                phenomenonURI);
    }

    /**
     * insertChildSensor,
     *
     * @param sensorID
     * @param platformID
     * @param networkID
     * @param observedProperty
     * @param uomCode
     * @param obsType          MEASUREMENT, COUNT, TEXT, CATEGORY, TRUTH
     * @param codeSpace        (will be only used for category as of now 0.0.7-SNAPSHOT)
     * @return
     */
//  FIXME: Never used!
    public static int insertChildSensor(String sensorID, String platformID,
                                        String networkID, String observedProperty, String uomCode, String obsType, String codeSpace) {
        SensorDescription mySml = new SensorDescription(sensorID, platformID,
                networkID, observedProperty, uomCode, obsType, codeSpace);
        entityType sensorType = SOSConstants.entityType.SENSOR;
        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/" + platformID + "/" + sensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + observedProperty;
        return insertSensor(sensorType, platformID, mySml, sensorURI,
                phenomenonURI, obsType);
    }

    /**
     * insertIndependentSensor, only support MEASUREMENT DEF observation type
     *
     * @param independencyIndicator
     * @param independencyGroup
     * @param uniqueSensorID
     * @param obsProp
     * @param uomCode
     * @param position
     * @return
     */
    @Deprecated
    public static int insertIndependentSensor(Boolean independencyIndicator,
                                              String independencyGroup, String uniqueSensorID, String obsProp,
                                              String uomCode, Double[] position) {
        SensorDescription mySml = null;

        if (!(position == null) && (position.length >= 2)) {
            mySml = new SensorDescription(independencyIndicator,
                    independencyGroup, uniqueSensorID, obsProp, uomCode,
                    position);
        }
        else {
            mySml = new SensorDescription(independencyIndicator,
                    independencyGroup, uniqueSensorID, obsProp, uomCode);
        }

        entityType sensorType = SOSConstants.entityType.INDEPENDENT;
        String sensorURI = URI_PREFIX + "/procedure/" + independencyGroup + "/"
                + uniqueSensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;
        return insertSensor(sensorType, uniqueSensorID, mySml, sensorURI,
                phenomenonURI);
    }

    /**
     * insertIndependentSensor with flexible phen def
     *
     * @param independencyIndicator
     * @param independencyGroup
     * @param uniqueSensorID
     * @param obsProp
     * @param uomCode
     * @param position
     * @param obsType               MEASUREMENT, COUNT, TEXT, CATEGORY, TRUTH
     * @param codeSpace             (will be only used for category as of now 0.0.7-SNAPSHOT)
     * @return
     */
    public static int insertIndependentSensor(Boolean independencyIndicator,
                                              String independencyGroup, String uniqueSensorID, String obsProp,
                                              String uomCode, Double[] position, String obsType, String codeSpace) {
        SensorDescription mySml = null;

        if (!(position == null) && (position.length >= 2)) {
            mySml = new SensorDescription(independencyIndicator,
                    independencyGroup, uniqueSensorID, obsProp, uomCode,
                    position, obsType, codeSpace);
        }
        else {
            mySml = new SensorDescription(independencyIndicator,
                    independencyGroup, uniqueSensorID, obsProp, uomCode, obsType, codeSpace);
        }

        entityType sensorType = SOSConstants.entityType.INDEPENDENT;
        String sensorURI = URI_PREFIX + "/procedure/" + independencyGroup + "/" + uniqueSensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;

        return insertSensor(sensorType, uniqueSensorID, mySml, sensorURI,
                phenomenonURI, obsType);
    }

    /**
     * takes a uniqueSensorID, some sophisticated netowk/platform/sensor prep
     * creates insertsensor post/soap xml request based on type indicator and
     * sensorml description
     *
     * @param sensorType
     * @param uniqueSensorID
     * @param mySml
     * @param sensorURI
     * @param phenomenonURI
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    @Deprecated
    public static int insertSensor(entityType sensorType,
                                   String uniqueSensorID, SensorDescription mySml, String sensorURI,
                                   String phenomenonURI) {

        logger.debug("Insert Sensor:\n"
                        + "SensorType: " + sensorType + "\n"
                        + "uniqueSensorID: " + uniqueSensorID + "\n"
                        + "sensorURI: " + sensorURI + "\n"
                        + "phenomenonURI: " + phenomenonURI
        );

        int returnCode = 5;

        // build InsertSensor xml request
        StringBuilder insertXML = new StringBuilder();
        // default headers, schema and soap envelope
        insertXML.append(SOSConstants.InsertSensorHeaders);
        // sensorML
        insertXML.append(mySml.getSensorML());
        // closetag
        insertXML.append("</swes:procedureDescription>\n");

        // if it is a Network Sensor placeholder, then there's no obs type def, sampling point or phenomenon
        if (!sensorType.equals(SOSConstants.entityType.NETWORK)) {
            // metadata
            insertXML.append("<swes:observableProperty>" + phenomenonURI + "</swes:observableProperty>\n");

            insertXML.append("<swes:metadata>\n<sos:SosInsertionMetadata>\n<sos:observationType>"
                    + SOSConstants.MEASUREMENT_OBS_DEF + "</sos:observationType>\n"
                    + "<sos:featureOfInterestType>"
                    + SOSConstants.SAMPLINGPOINT_DEF
                    + "</sos:featureOfInterestType>\n"
                    + "</sos:SosInsertionMetadata>\n" + "</swes:metadata>\n");

        }
        else {
            // here better logic, could be omitted actually?!
            // phenomenonURI = URI_PREFIX + "/phenomenon/collection/" + uniqueSensorID;
        }

        // "footer", closing soap envelop
        insertXML.append("</swes:InsertSensor>\n");

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpPost httppost = new HttpPost(sos_url_pox);

        String responseBody = "";
        HttpResponse response;
        HttpEntity entity;

        try {
            // execute and parse the response
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");

            entity = new StringEntity(insertXML.toString(), "UTF-8");
            httppost.setEntity(entity);

            logger.debug("InsertSensor: Request\n"
                    + "----------------------------------------\n"
                    + insertXML.toString() + "\n"
                    + "----------------------------------------");

            response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }

            logger.debug("InsertSensor: Response"
                    + "----------------------------------------"
                    + responseBody + "\n"
                    + "----------------------------------------");

            if (responseBody.contains("ows:Exception")) {

                if (responseBody.contains("InvalidParameterValue")) {
                    logger.error("insertSensor: InvalidParameterValue");
                    returnCode = 2;
                }
                else {
                    logger.error("insertSensor: some other ows:Exception");
                    returnCode = 5;
                }
            }
            else {
                if (responseBody.contains("swes:InsertSensorResponse")) {

                    if (responseBody.contains(uniqueSensorID)) {
                        logger.info("insertSensor: sensorID " + uniqueSensorID
                                + " inserted");
                        returnCode = 0;
                    }
                    else {
                        logger.error("insertSensor: unexpected error, swes response does not contain sensor ID");
                        returnCode = 5;
                    }
                }
                else {
                    logger.error("insertSensor: dont't know what to write, unlikely to come along here?");
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.error("insertSensor: error executing http get request - IOException", e);
            returnCode = 4;

        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }
        return returnCode;
    }

    /**
     * takes a uniqueSensorID, some sophisticated netowk/platform/sensor prep
     * creates insertsensor post/soap xml request based on type indicator and
     * sensorml description with obsType (TRUTH, MEAS, COUNT, TEXT, CATEGORY)
     * therefore must have actual concrete observableProperty decalred,
     * for NETWORK or PLATFORM we'll check if phenomenonURI and obsType are set
     * to omit metadata in the end
     *
     * @param sensorType
     * @param uniqueSensorID
     * @param mySml
     * @param sensorURI
     * @param phenomenonURI
     * @param obsType        with (TRUTH, MEAS, COUNT, TEXT, CATEGORY)
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    public static int insertSensor(entityType sensorType,
                                   String uniqueSensorID, SensorDescription mySml, String sensorURI,
                                   String phenomenonURI, String obsType) {

        int returnCode = 5;

        // build InsertSensor xml request
        StringBuilder insertXML = new StringBuilder();
        // default headers, schema and soap envelope
        insertXML.append(SOSConstants.InsertSensorHeaders);
        // sensorML
        insertXML.append(mySml.getSensorML());
        // closetag
        insertXML.append("</swes:procedureDescription>\n");

        // only if it is NOT a Network/Platform Sensor placeholder and those NETWORK/PLATFORM have null/empty obs type def, and phenomenon
        if (!(((sensorType.equals(SOSConstants.entityType.NETWORK)) || (sensorType.equals(SOSConstants.entityType.PLATFORM)))
                && ((phenomenonURI == null || phenomenonURI.isEmpty()) &&
                (obsType == null || obsType.isEmpty())))) {

            insertXML.append("<swes:observableProperty>" + phenomenonURI + "</swes:observableProperty>\n");

            insertXML.append("<swes:metadata>\n<sos:SosInsertionMetadata>\n");

            insertXML.append("<sos:observationType>" + getObsTypeURI(obsType) + "</sos:observationType>\n");

            insertXML.append("<sos:featureOfInterestType>"
                    + SOSConstants.SAMPLINGPOINT_DEF
                    + "</sos:featureOfInterestType>\n"
                    + "</sos:SosInsertionMetadata>\n" + "</swes:metadata>\n");
        }


        // "footer", closing soap envelop
        insertXML.append("</swes:InsertSensor>\n");

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpPost httppost = new HttpPost(sos_url_pox);

        String responseBody = "";
        HttpResponse response;
        HttpEntity entity;

        try {
            // execute and parse the response
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");

            entity = new StringEntity(insertXML.toString(), "UTF-8");
            httppost.setEntity(entity);

            logger.debug("InsertSensor: Request");
            logger.debug("----------------------------------------");
            logger.debug(insertXML.toString());
            logger.debug("----------------------------------------");

            response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }

            logger.debug("InsertSensor: Response");
            logger.debug("----------------------------------------");
            logger.debug(responseBody);
            logger.debug("----------------------------------------");

            if (responseBody.contains("ows:Exception")) {

                if (responseBody.contains("InvalidParameterValue")) {
                    logger.error("insertSensor: InvalidParameterValue");
                    returnCode = 2;
                }
                else {
                    logger.error("insertSensor: some other ows:Exception");
                    logger.debug(responseBody);
                    returnCode = 5;
                }
            }
            else {
                if (responseBody.contains("swes:InsertSensorResponse")) {

                    if (responseBody.contains(uniqueSensorID)) {
                        logger.info("insertSensor: sensorID " + uniqueSensorID
                                + " inserted");
                        returnCode = 0;
                    }
                    else {
                        logger.error("insertSensor: unexpected error, swes response does not contain sensor ID");
                        logger.debug(responseBody);
                        returnCode = 5;
                    }
                }
                else {
                    logger.error("insertSensor: dont't know what to write, unlikely to come along here?");
                    logger.debug(responseBody);
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.error("insertSensor: error executing http get request - IOException");
            e.printStackTrace();
            returnCode = 4;

        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }
        return returnCode;
    }

    /**
     * Is that method used somewhere?
     * <p/>
     * takes a uniqueSensorID, assume independent group or
     * netowk/platform/sensor does an update of generic sensor
     *
     * @param group
     * @param uniqueSensorID
     * @param observedProperty
     * @param uomCode
     * @return 0 - updated, 2 - InvalidParam/didn't exist, 4 - HTTP ERROR, 5 -
     * GENERIC OWS LOGIC ERRROR (should not really happen)
     */
    @Deprecated
    public static int updateSensor(String group, String uniqueSensorID,
                                   String observedProperty, String uomCode) {

        int returnCode = 5;
        SensorDescription mySml = new SensorDescription(true, group,
                uniqueSensorID, observedProperty, uomCode);

        String independencyGroup = "demogroup";
        String sensorURI = URI_PREFIX + "/procedure/" + independencyGroup + "/"
                + uniqueSensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + observedProperty;

        // build InsertSensor xml request
        StringBuilder updateXML = new StringBuilder();
        // default headers, schema and soap envelope
        updateXML.append(SOSConstants.UpdateSensorHeaders);

        updateXML
                .append("<swes:procedure>"
                        + sensorURI
                        + "</swes:procedure>"
                        + "<swes:procedureDescriptionFormat>http://www.opengis.net/sensorML/1.0.1</swes:procedureDescriptionFormat>\n"
                        + "<swes:description>\n" + "<swes:SensorDescription>\n"
                        + "<swes:data>\n");

        // sensorML
        updateXML.append(mySml.getSensorML());
        // metadata and "footer", closing soap envelop
        updateXML.append("</swes:data>\n" + "</swes:SensorDescription>\n"
                + "</swes:description>\n" + "</swes:UpdateSensorDescription>\n");

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpPost httppost = new HttpPost(sos_url_pox);
        String responseBody = "";
        HttpResponse response;
        HttpEntity entity;

        try {
            // execute and parse the response
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");

            entity = new StringEntity(updateXML.toString(), "UTF-8");
            httppost.setEntity(entity);
            logger.debug("updateSensor: UpdateSensorDescription Request");
            logger.debug("----------------------------------------");
            logger.debug(updateXML.toString());
            logger.debug("----------------------------------------");

            response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }

            logger.debug("updateSensor: UpdateSensorDescription Response");
            logger.debug("----------------------------------------");
            logger.debug(responseBody);
            logger.debug("----------------------------------------");

            if (responseBody.contains("ows:Exception")) {

                if (responseBody.contains("InvalidParameterValue")) {
                    logger.error("updateSensor: InvalidParameterValue - sensorID "
                            + uniqueSensorID
                            + " may not exist, and therefore can't be updated");
                    returnCode = 2;
                }
                else {
                    logger.error("updateSensor: some other ows:Exception");
                    logger.debug(responseBody);
                    returnCode = 5;
                }
            }
            else {
                if (responseBody
                        .contains("swes:UpdateSensorDescriptionResponse")) {

                    if (responseBody.contains(uniqueSensorID)) {
                        logger.info("updateSensor: sensorID " + uniqueSensorID
                                + " updated");
                        returnCode = 0;
                    }
                    else {
                        logger.error("updateSensor: unexpected error, swes response does not contain sensor ID");
                        logger.debug(responseBody);
                        returnCode = 5;
                    }
                }
                else {
                    logger.error("updateSensor: dont't know what to write, unlikely to come along here?");
                    logger.debug(responseBody);
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.error("updateSensor: error executing http post request - IOException");
            e.printStackTrace();
            returnCode = 4;

        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }
        return returnCode;

    }

    /**
     * crappy little helper, could be enhanced with the XMLStreamParser from Play experiments
     *
     * @return 0
     */
    public static Set<String> listSensors() {

        logger.debug("listSensors only provides coarse data from Capabilities: ");
        Set<String> sensorList = new HashSet<String>();

        StringBuilder kvpRequestParams = new StringBuilder();
        kvpRequestParams.append("service=" + "SOS");
        kvpRequestParams.append("&request=" + "GetCapabilities");
        kvpRequestParams.append("&AcceptVersions=" + "2.0.0");

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpGet httpget = new HttpGet(sos_url_kvp + kvpRequestParams.toString());

        String responseBody = "";
        HttpResponse response;

        logger.debug("listSensors: executing request: " + httpget.getURI());

        try {
            // execute and parse the response
            response = httpclient.execute(httpget);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }
            logger.debug("listSensors: ");
            logger.debug("----------------------------------------");
            logger.debug(responseBody);
            logger.debug("----------------------------------------");

            if (responseBody.contains("ows:Exception")) {

                if (responseBody.contains("InvalidParameterValue")) {
                    logger.error("listSensors: InvalidParameterValue");
                }
                else {
                    logger.error("listSensors: some other ows:Exception");
                    logger.debug(responseBody);
                }
            }
            else {
                if (responseBody.contains("sos:Capabilities")) {
                    String sensorURI = URI_PREFIX + "/procedure/";
                    ByteArrayInputStream ins = null;
                    InputStreamReader r = null;
                    BufferedReader br = null;
                    // String line;
                    try {
                        ins = new ByteArrayInputStream(
                                responseBody.getBytes("UTF-8"));
                        r = new InputStreamReader(ins, "UTF-8");
                        br = new BufferedReader(r);

                        // TODO here we could parse with a stream reader

                        while ((line = br.readLine()) != null) {

                            if (line.contains("<ows:Value>" + sensorURI)) {
                                // Matcher m = p.matcher(line);
                                // if (m.find()) {
                                // sensorList.add(m.group(1));
                                // }
                                sensorList.add(line);
                            }
                        }
                    }
                    finally {
                        br.close();
                        r.close();
                        ins.close();
                    }
                }
                else {
                    logger.error("listSensors: dont't know what to write, unlikely to come along here?");
                    logger.debug(responseBody);
                }
            }
        }
        catch (IOException e) {
            logger.error("deleteSensor: error executing http get request - IOException");
            e.printStackTrace();

        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return sensorList;
    }

    /**
     * networks can, but don't need to have observations, platforms could have battery level
     * or logmessages(TextObs) as observations, but you could also add those as childsensors
     *
     * @param platformID
     * @param networkID
     * @param obsProp
     * @param uomCode
     * @param value
     * @param phenTime
     * @param position   Double array [long,lat,altitude]
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    public static int insertPlatformMeasurement(String platformID,
                                                String networkID, String obsProp, String uomCode, Double value,
                                                Date phenTime, Double[] position) {

        // if position is null, we could assume only HREF feature link, is
        // evaluated in OMXML generation
        // if phenTime should not be null here anymore

        // again, phenomena and their uomCodes should be held centrally to be
        // looked up

        entityType sensorType = SOSConstants.entityType.PLATFORM;

        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/" + platformID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;
        String offeringURI = URI_PREFIX + "/offering/" + networkID + "/" + platformID;
        // TODO check featureURI generation overall methods
        String featureURI = URI_PREFIX + "/feature/" + networkID + "/" + platformID;

        // TODO add different OBSTYPE defs for Observations
        ObservationDescription myOMXML = new ObservationDescription(phenTime,
                offeringURI.toLowerCase(), sensorURI, phenomenonURI, uomCode, value,
                featureURI, position);

        return insertObservation(myOMXML, offeringURI);
    }

    /**
     * Measurement for childsensors
     *
     * @param sensorID
     * @param platformID
     * @param networkID
     * @param obsProp
     * @param uomCode
     * @param value      Double value
     * @param phenTime
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    public static int insertChildSensorMeasurement(String sensorID,
                                                   String platformID, String networkID, String obsProp,
                                                   String uomCode, Double value, Date phenTime) {

        // if phenTime should not be null here anymore
        // again, phenomena and their uomCodes should be held centrally to be
        // looked up
        entityType sensorType = SOSConstants.entityType.SENSOR;

        String sensorURI = URI_PREFIX + "/procedure/" + networkID + "/" + platformID + "/" + sensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;
        String offeringURI = URI_PREFIX + "/offering/" + networkID + "/" + platformID + "/" + sensorID;

        // position should be null, we assume PLATFORM HREF feature link, is
        // evaluated in OMXML generation
        // TODO check feature uri generation in all methods
        String featureURI = URI_PREFIX + "/feature/" + networkID + "/" + platformID;
        ObservationDescription myOMXML = new ObservationDescription(phenTime,
                offeringURI, sensorURI, phenomenonURI, uomCode, value,
                featureURI, null);

        return insertObservation(myOMXML, offeringURI);
    }

    /**
     * Measurement for independent group sensors
     *
     * @param independencyIndicator
     * @param independencyGroup
     * @param uniqueSensorID
     * @param obsProp
     * @param uomCode
     * @param value
     * @param phenTime
     * @param position              Double array [long,lat,altitude]
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    public static int insertIndependentMeasurement(
            Boolean independencyIndicator, String independencyGroup,
            String uniqueSensorID, String obsProp, String uomCode,
            Double value, Date phenTime, Double[] position) {

        if (position == null) {
            // if position is null, we could assume only HREF feature link
        }
        if (phenTime == null) {
            // if phenTime is null, we could create timestamp here
        }
        // again, phenomena and their uomCodes should be held centrally to be
        // looked up

        entityType sensorType = SOSConstants.entityType.INDEPENDENT;
        String sensorURI = URI_PREFIX + "/procedure/" + independencyGroup + "/" + uniqueSensorID;
        String phenomenonURI = URI_PREFIX + "/phenomenon/" + obsProp;
        String offeringURI = URI_PREFIX + "/offering/" + independencyGroup + "/" + uniqueSensorID;
        String featureURI = URI_PREFIX + "/feature/" + independencyGroup + "/" + uniqueSensorID;

        ObservationDescription myOMXML = new ObservationDescription(phenTime,
                offeringURI, sensorURI, phenomenonURI, uomCode, value,
                featureURI, position);

        return insertObservation(myOMXML, offeringURI);
    }

    /**
     * insertObservation, does the actual call via POST/XML
     *
     * @param myOMXML
     * @param offeringURI
     * @return 0 - inserted, 2 - InvalidParam used, 4 - HTTP ERROR, 5 - GENERIC
     * OWS LOGIC ERRROR (should not really happen)
     */
    public static int insertObservation(ObservationDescription myOMXML,
                                        String offeringURI) {
        int returnCode = 5;

        StringBuilder insertXML = new StringBuilder();
        // default headers, schema and soap envelope
        insertXML.append(SOSConstants.InsertObservationHeaders);
        // O&M insert XML
        insertXML.append(myOMXML.getOM_Member());

        // "footer", closing soap envelop
        insertXML.append("</sos:InsertObservation>\n");

        // set the connection timeout value to xx milliseconds
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT_MS);

        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpPost httppost = new HttpPost(sos_url_pox);
        String responseBody = "";
        HttpResponse response;
        HttpEntity entity;

        try {
            // execute and parse the response
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
            entity = new StringEntity(insertXML.toString(), "UTF-8");

            httppost.setEntity(entity);
            logger.debug("InsertObservation: Request\n"
                            + "----------------------------------------\n"
                            + insertXML.toString() + "\n"
                            + "----------------------------------------"
            );

            response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    resEntity.getContent()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBody += line;
            }

            logger.debug("InsertObservation: Response\n"
                            + "----------------------------------------\n"
                            + responseBody + "\n"
                            + "----------------------------------------"
            );

            if (responseBody.contains("ows:Exception")) {
                if (responseBody.contains("InvalidParameterValue")) {
                    logger.error("InsertObservation: InvalidParameterValue");
                    returnCode = 2;
                }
                else {
                    logger.error("InsertObservation: some other ows:Exception");
                    returnCode = 5;
                }
            }
            else {
                if (responseBody.contains("sos:InsertObservationResponse")) {
                    logger.debug("InsertObservation: inserted");
                    returnCode = 0;
                }
                else {
                    logger.error("InsertObservation: dont't know what to write, unlikely to come along here?");
                    return 5;
                }
            }
        }
        catch (IOException e) {
            logger.error("InsertObservation: error executing http get request - IOException", e);
            returnCode = 4;
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }
        return returnCode;
    }

    /**
     * urlEncode() -- URL encode the string
     *
     * @param parameter
     * @return
     */
    private static String urlEncode(String parameter) {
        try {
            return URLEncoder.encode(parameter, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            logger.error("Error while encoding URI", e);
        }
        return "";
    }

    /**
     * fetches the full OGC DEF URI for OBSTYPE from SOSConstants
     *
     * @param typeofObservation = obsType MEASUREMENT, COUNT, TEXT, CATEGORY, TRUTH
     * @return
     */
    private static String getObsTypeURI(String typeofObservation) {

        // ObservationType MEASUREMENT, COUNT, TEXT, CATEGORY, TRUTH
        if (typeofObservation.equalsIgnoreCase("MEASUREMENT")) {

            return SOSConstants.MEASUREMENT_OBS_DEF;

        }
        else if (typeofObservation.equalsIgnoreCase("COUNT")) {

            return SOSConstants.COUNT_OBS_DEF;

        }
        else if (typeofObservation.equalsIgnoreCase("TEXT")) {

            return SOSConstants.TEXT_OBS_DEF;

        }
        else if (typeofObservation.equalsIgnoreCase("CATEGORY")) {

            return SOSConstants.CATEGORY_OBS_DEF;

        }
        else if (typeofObservation.equalsIgnoreCase("TRUTH")) {

            return SOSConstants.TRUTH_OBS_DEF;

        }
        else {

            // or null or empty?
            return SOSConstants.MEASUREMENT_OBS_DEF;

        }
    }
}
