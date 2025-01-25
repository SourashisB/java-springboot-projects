package com.trading.controller;

import com.trading.model.Portfolio;
import com.trading.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;
    
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }
    
    @GetMapping("/{userId}")
    public Portfolio getPortfolio(@PathVariable Long userId) {
        // Implementation
        return null;
    }
}