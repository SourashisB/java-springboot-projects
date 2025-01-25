package com.trading.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    
    @Version
    private Long version;
}