package com.barinventory.admin.dto;

 
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bar registration form data
 * Step 1: Basic information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarRegistrationDTO {
    
    // Step 1: Basic Information
    private String barName;
    private String barType;
    private String ownerName;
    private String contactNumber;
    private String email;
    
    // Step 1: Address
    private String addressLine;
    private String city;
    private String state;
    private String pinCode;
    
    // Step 1: License & Compliance
    private String licenseNumber;
    private String licenseType;
    private LocalDate licenseExpiryDate;
    private String gstin;
    
    // Step 1: Operational Configuration
    private String shiftConfig;
    private LocalDate openingDate;
    
    // Meta
    private Long barId;  // Set after creation
    private int currentStep = 1;
}