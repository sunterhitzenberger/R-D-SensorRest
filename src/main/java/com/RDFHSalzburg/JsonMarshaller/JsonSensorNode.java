/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.RDFHSalzburg.JsonMarshaller;

import com.RDFHSalzburg.sensorrest.SensorNode;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Admin
 */
public class JsonSensorNode {
    @SerializedName("id")
    @Expose
    private Integer id = -1;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("latitude")
    @Expose
    private Double latitude;
    @SerializedName("longitude")
    @Expose
    private Double longitude;
    @SerializedName("altitude")
    @Expose
    private Double altitude;
    @SerializedName("version")
    @Expose
    private String version;
    @SerializedName("battery")
    @Expose
    private BatteryInformation battery = null;
    @SerializedName("measurements")
    @Expose
    private List<Integer> measurements = null;

    public JsonSensorNode(SensorNode node){
        if(node != null){
            this.id = node.getIdSensorNode();
            this.address = node.getExtendedAddress();
            this.name = node.getName();
            this.description = node.getDescription();
            this.latitude = node.getLatitude();
            this.longitude = node.getLongitude();
            this.altitude = node.getAltitude();
        }
        version = "test";
        measurements = new ArrayList<Integer>();
        measurements.add(111);
        measurements.add(112);
        measurements.add(113);
        battery = new BatteryInformation();
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public Integer getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public String getVersion() {
        return version;
    }

    public BatteryInformation getBattery() {
        return battery;
    }

    public void setBattery(BatteryInformation battery) {
        this.battery = battery;
    }
    
    public class BatteryInformation {
        @SerializedName("voltage")
        @Expose
        String voltage = "";
        
        @SerializedName("percent")
        @Expose
        int percent = 0;
        
        @SerializedName("condition")
        @Expose
        String condition = "";

        public BatteryInformation() {
            voltage = "10.8V";
            percent = 77;
            condition = "good";
        }
        
        public String getVoltage() {
            return voltage;
        }

        public void setVoltage(String voltage) {
            this.voltage = voltage;
        }

        public Integer getPercent() {
            return percent;
        }

        public void setPercent(Integer percent) {
            this.percent = percent;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }
    }
}
