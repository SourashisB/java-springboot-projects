package com.trading.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    
    @ManyToOne
    private User user;
    
    public enum OrderType {
        BUY, SELL
    }
    
    public enum OrderStatus {
        PENDING, EXECUTED, CANCELLED, FAILED
    }
}