package com.trading.service;

import com.trading.model.Order;
import com.trading.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TradingService tradingService;
    
    public OrderService(OrderRepository orderRepository, TradingService tradingService) {
        this.orderRepository = orderRepository;
        this.tradingService = tradingService;
    }
    
    // Async method demonstration
    @Async
    public CompletableFuture<Order> processOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            validateOrder(order);
            order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
            return tradingService.executeOrder(order);
        });
    }
    
    // Method overloading demonstration
    public Order createOrder(String symbol, double quantity, double price) {
        return createOrder(symbol, quantity, price, Order.OrderType.BUY);
    }
    
    public Order createOrder(String symbol, double quantity, double price, Order.OrderType type) {
        Order order = new Order();
        order.setSymbol(symbol);
        order.setQuantity(BigDecimal.valueOf(quantity));
        order.setPrice(BigDecimal.valueOf(price));
        order.setOrderType(type);
        return order;
    }
    
    private void validateOrder(Order order) {
        // Validation logic
    }
}