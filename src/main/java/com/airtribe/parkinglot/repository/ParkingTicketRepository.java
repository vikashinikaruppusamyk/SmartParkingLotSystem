package com.airtribe.parkinglot.repository;

import com.airtribe.parkinglot.entity.ParkingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {
    Optional<ParkingTicket> findByVehicle_VehicleIdAndExitTimeIsNull(Long vehicleId);
}