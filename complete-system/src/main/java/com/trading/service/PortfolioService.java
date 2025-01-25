package com.trading.service;

import com.trading.model.Order;
import com.trading.model.Portfolio;
import com.trading.model.User;
import com.trading.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PricingService pricingService;
    
    public PortfolioService(PortfolioRepository portfolioRepository, PricingService pricingService) {
        this.portfolioRepository = portfolioRepository;
        this.pricingService = pricingService;
    }

    @Transactional
    public void updatePortfolio(Order order) {
        Portfolio portfolio = getOrCreatePortfolio(order.getUser());
        Map<String, BigDecimal> holdings = new ConcurrentHashMap<>(portfolio.getHoldings());
        
        BigDecimal currentQuantity = holdings.getOrDefault(order.getSymbol(), BigDecimal.ZERO);
        if (order.getOrderType() == Order.OrderType.BUY) {
            holdings.put(order.getSymbol(), currentQuantity.add(order.getQuantity()));
        } else {
            holdings.put(order.getSymbol(), currentQuantity.subtract(order.getQuantity()));
        }
        
        portfolio.setHoldings(holdings);
        updatePortfolioValue(portfolio);
        portfolioRepository.save(portfolio);
    }

    private Portfolio getOrCreatePortfolio(User user) {
        return portfolioRepository.findByUser(user)
                .orElseGet(() -> {
                    Portfolio newPortfolio = new Portfolio();
                    newPortfolio.setUser(user);
                    newPortfolio.setHoldings(new ConcurrentHashMap<>());
                    return newPortfolio;
                });
    }

    private void updatePortfolioValue(Portfolio portfolio) {
        BigDecimal totalValue = portfolio.getHoldings().entrySet().stream()
                .map(entry -> pricingService.getCurrentPrice(entry.getKey())
                        .multiply(entry.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        portfolio.setTotalValue(totalValue);
    }
}