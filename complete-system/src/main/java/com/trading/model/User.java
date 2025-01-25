package com.trading.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    private String password;
    private String email;
    private BigDecimal balance;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Portfolio> portfolios;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Order> orders;
}