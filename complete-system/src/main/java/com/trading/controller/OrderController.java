package com.trading.controller;

import com.trading.model.Order;
import com.trading.service.OrderService;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public CompletableFuture<Order> createOrder(@RequestBody Order order) {
        return orderService.processOrder(order);
    }
    
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        // Implementation
        return null;
    }
}