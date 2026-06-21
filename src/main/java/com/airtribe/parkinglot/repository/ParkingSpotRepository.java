package com.airtribe.parkinglot.repository;

import com.airtribe.parkinglot.entity.ParkingSpot;
import com.airtribe.parkinglot.enums.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByVehicleTypeAndIsAvailableTrue(VehicleType vehicleType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSpot s WHERE s.vehicleType = :vehicleType AND s.isAvailable = true")
    List<ParkingSpot> findAvailableSpotsForUpdate(@Param("vehicleType") VehicleType vehicleType);
}

