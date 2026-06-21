package com.airtribe.parkinglot.controller;

import com.airtribe.parkinglot.dto.CheckInRequest;
import com.airtribe.parkinglot.dto.CheckOutRequest;
import com.airtribe.parkinglot.entity.ParkingSpot;
import com.airtribe.parkinglot.entity.ParkingTicket;
import com.airtribe.parkinglot.enums.VehicleType;
import com.airtribe.parkinglot.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @PostMapping("/spots")
    public ResponseEntity<ParkingSpot> createSpot(@RequestParam VehicleType vehicleType) {
        ParkingSpot saved = parkingService.createSpot(vehicleType);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PostMapping("/checkin")
    public ResponseEntity<ParkingTicket> checkIn(@RequestBody CheckInRequest request) {
        ParkingTicket ticket = parkingService.checkIn(
                request.getLicensePlate(),
                request.getVehicleType());
        return new ResponseEntity<>(ticket, HttpStatus.CREATED);
    }

    @PostMapping("/checkout")
    public ResponseEntity<ParkingTicket> checkOut(@RequestBody CheckOutRequest request) {
        ParkingTicket ticket = parkingService.checkOut(request.getLicensePlate());
        return new ResponseEntity<>(ticket, HttpStatus.OK);
    }

    @GetMapping("/availability")
    public ResponseEntity<Long> getAvailability(@RequestParam VehicleType vehicleType) {
        long count = parkingService.getAvailableSpotCount(vehicleType);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}