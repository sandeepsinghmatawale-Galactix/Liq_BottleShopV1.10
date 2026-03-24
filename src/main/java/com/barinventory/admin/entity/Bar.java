package com.barinventory.admin.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bars")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"sessions", "productPrices", "wells", "users"})
public class Bar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long barId;

    // ── BASIC INFO ──────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 100)
    private String barName;

    @Column(length = 50)
    private String barType;          // STANDALONE, HOTEL_BAR, RESTAURANT_BAR, CLUB

    @Column(length = 100)
    private String ownerName;

    @Column(length = 20)
    private String contactNumber;

    @Column(length = 100)
    private String email;

    // ── ADDRESS ─────────────────────────────────────────────
    @Column(length = 200)
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pinCode;

    // convenience getter used in templates
    public String getLocation() {
        return city != null ? city + (state != null ? ", " + state : "") : addressLine;
    }

    // ── LICENSE & COMPLIANCE ────────────────────────────────
    @Column(length = 50)
    private String licenseNumber;

    @Column(length = 50)
    private String licenseType;      // FL-2, FL-3, CL-9, etc.

    private LocalDate licenseExpiryDate;

    @Column(length = 20)
    private String gstin;

    // ── OPERATIONAL CONFIG ──────────────────────────────────
    @Column(length = 20)
    private String shiftConfig;      // SINGLE, DOUBLE

    private LocalDate openingDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;  // false until fully onboarded

    @Column(nullable = false)
    @Builder.Default
    private Boolean setupComplete = false;   // true after opening stock saved

    @Column(length = 30)
    @Builder.Default
    private String onboardingStep = "BASIC_INFO"; // tracks wizard progress

    // ── RELATIONSHIPS ────────────────────────────────────────
    @OneToMany(mappedBy = "bar", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BarProductPrice> productPrices;

    @OneToMany(mappedBy = "bar", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BarWell> wells = new ArrayList<>();

 // ✅ Correct — mapped through UserBarAccess
    @OneToMany(mappedBy = "bar", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<UserBarAccess> userAccesses = new ArrayList<>();

    @OneToMany(mappedBy = "bar")
    @JsonIgnore
    private List<InventorySession> sessions;
    
    public String getStatusClass() {

        if (active == null || !active) {
            return "onboard-pending";
        }

        if (setupComplete != null && setupComplete) {
            return "onboard-active";
        }

        return "onboard-stock";
    }
}