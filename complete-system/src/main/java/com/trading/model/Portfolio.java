package com.trading.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private User user;
    
    @ElementCollection
    @CollectionTable(name = "portfolio_holdings")
    @MapKeyColumn(name = "symbol")
    @Column(name = "quantity")
    private Map<String, BigDecimal> holdings;
    
    private BigDecimal totalValue;
    private BigDecimal profitLoss;
}