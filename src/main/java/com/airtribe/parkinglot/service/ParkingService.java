package com.airtribe.parkinglot.service;

import com.airtribe.parkinglot.entity.ParkingSpot;
import com.airtribe.parkinglot.entity.ParkingTicket;
import com.airtribe.parkinglot.entity.Vehicle;
import com.airtribe.parkinglot.enums.VehicleType;
import com.airtribe.parkinglot.exception.NoSpotAvailableException;
import com.airtribe.parkinglot.exception.TicketNotFoundException;
import com.airtribe.parkinglot.repository.ParkingSpotRepository;
import com.airtribe.parkinglot.repository.ParkingTicketRepository;
import com.airtribe.parkinglot.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final VehicleRepository vehicleRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;

    @Transactional
    public ParkingTicket checkIn(String licensePlate, VehicleType vehicleType) {

        // Step 1: Find or create the vehicle
        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate)
                .orElseGet(() -> {
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setLicensePlate(licensePlate);
                    newVehicle.setVehicleType(vehicleType);
                    return vehicleRepository.save(newVehicle);
                });

        // Step 2: Find an available spot for this vehicle type
        List<ParkingSpot> availableSpots =
                parkingSpotRepository.findByVehicleTypeAndIsAvailableTrue(vehicleType);

        if (availableSpots.isEmpty()) {
            throw new NoSpotAvailableException(
                    "No available spot for vehicle type: " + vehicleType);
        }

        ParkingSpot spot = availableSpots.get(0);

        // Step 3: Mark spot as occupied
        spot.setAvailable(false);
        parkingSpotRepository.save(spot);

        // Step 4: Create the ticket
        ParkingTicket ticket = new ParkingTicket();
        ticket.setVehicle(vehicle);
        ticket.setParkingSpot(spot);
        ticket.setEntryTime(LocalDateTime.now());

        return parkingTicketRepository.save(ticket);
    }

    @Transactional
    public ParkingTicket checkOut(String licensePlate) {

        // Step 1: Find the vehicle
        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new TicketNotFoundException(
                        "No vehicle found with license plate: " + licensePlate));

        // Step 2: Find their active (open) ticket
        ParkingTicket ticket = parkingTicketRepository
                .findByVehicle_VehicleIdAndExitTimeIsNull(vehicle.getVehicleId())
                .orElseThrow(() -> new TicketNotFoundException(
                        "No active ticket found for vehicle: " + licensePlate));

        // Step 3: Set exit time
        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);

        // Step 4: Calculate fare
        BigDecimal fare = calculateFare(ticket.getEntryTime(), exitTime, vehicle.getVehicleType());
        ticket.setFare(fare);

        // Step 5: Free up the spot
        ParkingSpot spot = ticket.getParkingSpot();
        spot.setAvailable(true);
        parkingSpotRepository.save(spot);

        return parkingTicketRepository.save(ticket);
    }

    private BigDecimal calculateFare(LocalDateTime entryTime, LocalDateTime exitTime, VehicleType vehicleType) {
        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0); // round up partial hour
        if (hours == 0) hours = 1; // minimum 1 hour charge

        BigDecimal ratePerHour = switch (vehicleType) {
            case BIKE -> BigDecimal.valueOf(10);
            case CAR -> BigDecimal.valueOf(20);
            case BUS -> BigDecimal.valueOf(50);
        };

        return ratePerHour.multiply(BigDecimal.valueOf(hours));
    }
}