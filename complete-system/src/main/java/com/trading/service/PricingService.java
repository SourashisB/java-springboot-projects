package com.trading.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;

@Service
public class PricingService {
    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();
    private final Map<String, Thread> priceUpdateThreads = new ConcurrentHashMap<>();
    
    public void startPriceUpdates(String symbol) {
        Thread updateThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                updatePrice(symbol);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        priceUpdateThreads.put(symbol, updateThread);
        updateThread.start();
    }
    
    private void updatePrice(String symbol) {
        BigDecimal currentPrice = prices.getOrDefault(symbol, BigDecimal.valueOf(100.0));
        double change = (Math.random() - 0.5) * 2.0;
        prices.put(symbol, currentPrice.add(BigDecimal.valueOf(change)));
    }
    
    public BigDecimal getCurrentPrice(String symbol) {
        return prices.getOrDefault(symbol, BigDecimal.valueOf(100.0));
    }
}