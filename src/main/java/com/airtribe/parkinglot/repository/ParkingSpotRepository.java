package com.airtribe.parkinglot.repository;

import com.airtribe.parkinglot.entity.ParkingSpot;
import com.airtribe.parkinglot.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByVehicleTypeAndIsAvailableTrue(VehicleType vehicleType);
}