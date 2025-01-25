package com.trading.repository;

import com.trading.model.Order;
import com.trading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findBySymbol(String symbol);
    List<Order> findByStatus(Order.OrderStatus status);
}