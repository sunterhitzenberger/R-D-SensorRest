/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.sensorrest;

import java.sql.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Admin
 */
@XmlRootElement
public class SensorNode {
    Integer idSensorNode = 1;
    String ExtendedAddress = "aaa";
    String Name = "bbb";
    String Description = "ccc";
    Double Longitude = 22.22;
    Double Latitude = 33.33;
    Double Altitude = 12.33;

    public SensorNode() {
    }

    public SensorNode(ResultSet rs) {
        try {
            // Load the database driver
            Class.forName("com.mysql.jdbc.Driver");

            idSensorNode = rs.getInt("idSensorNode");
            ExtendedAddress = rs.getString(2);
            Name = rs.getString("Name");
            Description = rs.getString("Description");
            Longitude = rs.getDouble("Longitude");
            Latitude = rs.getDouble("Latitude");
            Altitude = rs.getDouble("Altitude");

        }
        catch (SQLException ex) {
            System.out.println(ex.getMessage());
            //Logger.getLogger(SensorNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (ClassNotFoundException ex) {
            System.out.println(ex.getMessage());
            //Logger.getLogger(SensorNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public SensorNode(String name, String Description) {
        this.Name = name;
        this.Description = Description;
    }

    public Integer getIdSensorNode() {
        return idSensorNode;
    }

    public String getExtendedAddress() {
        return ExtendedAddress;
    }

    public String getName() {
        return Name;
    }

    public String getDescription() {
        return Description;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public Double getAltitude() {
        return Altitude;
    }


}
