package com.trading.service;

import com.trading.model.Order;
import com.trading.model.Trade;
import com.trading.repository.OrderRepository;
import com.trading.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TradingService {
    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final PortfolioService portfolioService;
    private final NotificationService notificationService;

    public TradingService(
            OrderRepository orderRepository,
            PricingService pricingService,
            PortfolioService portfolioService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.portfolioService = portfolioService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Order executeOrder(Order order) {
        try {
            BigDecimal currentPrice = pricingService.getCurrentPrice(order.getSymbol());
            
            if (isValidPrice(order, currentPrice)) {
                Trade trade = createTrade(order, currentPrice);
                order.setStatus(Order.OrderStatus.EXECUTED);
                portfolioService.updatePortfolio(order);
                
                notificationService.notify("Order executed: " + order.getId());
            } else {
                order.setStatus(Order.OrderStatus.FAILED);
                notificationService.notify("Order failed due to price mismatch: " + order.getId());
            }
            
            return orderRepository.save(order);
        } catch (Exception e) {
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            throw e;
        }
    }

    private boolean isValidPrice(Order order, BigDecimal currentPrice) {
        if (order.getOrderType() == Order.OrderType.BUY) {
            return currentPrice.compareTo(order.getPrice()) <= 0;
        } else {
            return currentPrice.compareTo(order.getPrice()) >= 0;
        }
    }

    private Trade createTrade(Order order, BigDecimal executionPrice) {
        Trade trade = new Trade();
        trade.setOrder(order);
        trade.setExecutionPrice(executionPrice);
        trade.setExecutionTime(LocalDateTime.now());
        trade.setExecutionId(UUID.randomUUID().toString());
        return trade;
    }
}