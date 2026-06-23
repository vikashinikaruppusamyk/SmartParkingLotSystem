package com.airtribe.parkinglot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckOutRequest {

    @NotBlank(message = "License plate is required")
    private String licensePlate;
}