package com.barinventory.admin.entity;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String productName;
    
    @Column(length = 50)
    private String category;
    
    @Column(length = 50)
    private String brand;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal volumeML;
    
    @Column(length = 20)
    private String unit = "BOTTLE";
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BarProductPrice> barPrices;

     
}