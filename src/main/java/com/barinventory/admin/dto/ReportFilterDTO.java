package com.barinventory.admin.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ReportFilterDTO {

    private String type; 
    // DAILY | WEEKLY | MONTHLY | QUARTERLY | YEARLY | CUSTOM

    private LocalDate date;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer month;
    private Integer year;

    private Integer quarter;
}