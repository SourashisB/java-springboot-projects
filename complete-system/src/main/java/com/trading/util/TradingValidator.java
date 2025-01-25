package com.trading.util;

import com.trading.model.Order;
import com.trading.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TradingValidator {
    
    public boolean validateOrder(Order order, User user) {
        if (order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        
        if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order price must be positive");
        }
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            validateBuyOrder(order, user);
        } else {
            validateSellOrder(order, user);
        }
        
        return true;
    }
    
    private void validateBuyOrder(Order order, User user) {
        BigDecimal totalCost = order.getPrice().multiply(order.getQuantity());
        if (user.getBalance().compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }
    
    private void validateSellOrder(Order order, User user) {
        // Validate if user has enough stocks to sell
        user.getPortfolios().stream()
                .flatMap(p -> p.getHoldings().entrySet().stream())
                .filter(e -> e.getKey().equals(order.getSymbol()))
                .findFirst()
                .ifPresentOrElse(
                    holding -> {
                        if (holding.getValue().compareTo(order.getQuantity()) < 0) {
                            throw new IllegalStateException("Insufficient stocks");
                        }
                    },
                    () -> { throw new IllegalStateException("Stock not found in portfolio"); }
                );
    }
}