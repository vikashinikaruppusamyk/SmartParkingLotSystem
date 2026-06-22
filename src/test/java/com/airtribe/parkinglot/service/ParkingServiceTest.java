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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private ParkingTicketRepository parkingTicketRepository;

    @InjectMocks
    private ParkingService parkingService;

    private Vehicle existingVehicle;

    @BeforeEach
    void setUp() {
        existingVehicle = new Vehicle();
        existingVehicle.setVehicleId(1L);
        existingVehicle.setLicensePlate("KA01AB1234");
        existingVehicle.setVehicleType(VehicleType.CAR);
    }

    @Test
    void checkIn_shouldCreateTicket_whenSpotAvailable() {
        // Arrange: vehicle exists, and one available spot exists
        when(vehicleRepository.findByLicensePlate("KA01AB1234"))
                .thenReturn(Optional.of(existingVehicle));

        ParkingSpot availableSpot = new ParkingSpot();
        availableSpot.setSpotId(1L);
        availableSpot.setVehicleType(VehicleType.CAR);
        availableSpot.setAvailable(true);

        when(parkingSpotRepository.findAvailableSpotsForUpdate(VehicleType.CAR))
                .thenReturn(List.of(availableSpot));

        when(parkingSpotRepository.save(any(ParkingSpot.class)))
                .thenReturn(availableSpot);

        ParkingTicket savedTicket = new ParkingTicket();
        savedTicket.setTicketId(1L);
        savedTicket.setVehicle(existingVehicle);
        savedTicket.setParkingSpot(availableSpot);
        savedTicket.setEntryTime(LocalDateTime.now());

        when(parkingTicketRepository.save(any(ParkingTicket.class)))
                .thenReturn(savedTicket);

        // Act
        ParkingTicket result = parkingService.checkIn("KA01AB1234", VehicleType.CAR);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTicketId());
        assertEquals(existingVehicle, result.getVehicle());
        assertFalse(availableSpot.isAvailable()); // spot should now be marked occupied
    }

    @Test
    void checkIn_shouldThrowException_whenNoSpotAvailable() {
        // Arrange: vehicle already exists, but no spots available
        when(vehicleRepository.findByLicensePlate("KA01AB1234"))
                .thenReturn(Optional.of(existingVehicle));

        when(parkingSpotRepository.findAvailableSpotsForUpdate(VehicleType.CAR))
                .thenReturn(Collections.emptyList());

        // Act + Assert
        assertThrows(NoSpotAvailableException.class, () ->
                parkingService.checkIn("KA01AB1234", VehicleType.CAR));
    }

    @Test
    void checkOut_shouldThrowException_whenNoActiveTicketFound() {
        // Arrange: vehicle exists, but no open ticket for them
        when(vehicleRepository.findByLicensePlate("KA01AB1234"))
                .thenReturn(Optional.of(existingVehicle));

        when(parkingTicketRepository.findByVehicle_VehicleIdAndExitTimeIsNull(1L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(TicketNotFoundException.class, () ->
                parkingService.checkOut("KA01AB1234"));
    }

    @Test
    void checkOut_shouldThrowException_whenVehicleNotFound() {
        // Arrange: vehicle was never registered/checked in
        when(vehicleRepository.findByLicensePlate("KA99ZZ9999"))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(TicketNotFoundException.class, () ->
                parkingService.checkOut("KA99ZZ9999"));
    }

    @Test
    void checkOut_shouldCalculateFareAndFreeSpot_whenActiveTicketExists() {
        // Arrange
        when(vehicleRepository.findByLicensePlate("KA01AB1234"))
                .thenReturn(Optional.of(existingVehicle));

        ParkingSpot occupiedSpot = new ParkingSpot();
        occupiedSpot.setSpotId(1L);
        occupiedSpot.setVehicleType(VehicleType.CAR);
        occupiedSpot.setAvailable(false);

        ParkingTicket activeTicket = new ParkingTicket();
        activeTicket.setTicketId(1L);
        activeTicket.setVehicle(existingVehicle);
        activeTicket.setParkingSpot(occupiedSpot);
        activeTicket.setEntryTime(LocalDateTime.now().minusHours(2)); // parked 2 hours ago

        when(parkingTicketRepository.findByVehicle_VehicleIdAndExitTimeIsNull(1L))
                .thenReturn(Optional.of(activeTicket));

        when(parkingSpotRepository.save(any(ParkingSpot.class)))
                .thenReturn(occupiedSpot);

        when(parkingTicketRepository.save(any(ParkingTicket.class)))
                .thenReturn(activeTicket);

        // Act
        ParkingTicket result = parkingService.checkOut("KA01AB1234");

        // Assert
        assertNotNull(result.getExitTime());
        assertEquals(BigDecimal.valueOf(40), result.getFare()); // 2 hours * ₹20/hour for CAR
        assertTrue(occupiedSpot.isAvailable()); // spot should be freed
    }

    @Test
    void getAvailableSpotCount_shouldReturnCorrectCount() {
        // Arrange
        ParkingSpot spot1 = new ParkingSpot();
        ParkingSpot spot2 = new ParkingSpot();

        when(parkingSpotRepository.findByVehicleTypeAndIsAvailableTrue(VehicleType.CAR))
                .thenReturn(List.of(spot1, spot2));

        // Act
        long count = parkingService.getAvailableSpotCount(VehicleType.CAR);

        // Assert
        assertEquals(2, count);
    }
}