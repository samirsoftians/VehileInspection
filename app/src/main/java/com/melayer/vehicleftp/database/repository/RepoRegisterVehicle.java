package com.melayer.vehicleftp.database.repository;

import com.melayer.vehicleftp.domain.Module;
import com.melayer.vehicleftp.domain.VehicleRegistration;

import java.util.List;
import java.util.Set;

/**
 * Created by root on 25/8/16.
 */
public interface RepoRegisterVehicle {
    void registerVehicle(VehicleRegistration registration);
    String getVehicleId(String vehicleNo)throws Exception;
    void updateFlag(String vehicleNo)throws Exception;
    void deleteTableData()throws Exception;
    Integer checkDataInspections(String vehicleNo)throws Exception;
    List<VehicleRegistration> uploadData(String userId)throws Exception;
    List<VehicleRegistration> uploadVehicleDataToServer(String userId)throws Exception;

    List<VehicleRegistration> selectAll() throws Exception;
}
