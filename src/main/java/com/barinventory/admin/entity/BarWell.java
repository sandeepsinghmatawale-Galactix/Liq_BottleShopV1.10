package com.barinventory.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bar_wells")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BarWell {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // ✅ ADDED: This is your bar_well_id
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bar_id", nullable = false)
    private Bar bar;
    
    @Column(nullable = false, length = 50)
    private String wellName;
    
    @Column(nullable = false)
    private boolean active = true;
    
    // ✅ Add this getter for easy reference
    public Long getBarWellId() {
        return this.id;
    }
}