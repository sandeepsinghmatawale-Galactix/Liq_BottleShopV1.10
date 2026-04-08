package com.barinventory.admin.dto;

import java.time.LocalDate;

import com.barinventory.admin.enums.BarSubscriptionStatus;

import lombok.Data;

@Data
public class BarSubscriptionRequest {

    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private Integer freeLoginLimit;
    private Integer freeLoginUsed;
    private Boolean resetFreeLogins;
    private Boolean hiddenOnPlatform;
    private Boolean adminBlocked;
    private String subscriptionNotes;
    private BarSubscriptionStatus subscriptionStatus;
}
