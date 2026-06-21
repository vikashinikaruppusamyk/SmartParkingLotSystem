package com.airtribe.parkinglot.dto;

import com.airtribe.parkinglot.enums.VehicleType;
import lombok.Data;

@Data
public class CheckInRequest {
    private String licensePlate;
    private VehicleType vehicleType;
}