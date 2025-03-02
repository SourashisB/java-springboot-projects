package com.trading.service.notification;
import com.trading.model.Order;
import com.trading.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    void notify(String message);
}

@Service
public class BaseNotificationService implements NotificationService {
    @Override
    public void notify(String message) {
        System.out.println("Base notification: " + message);
    }
}

// Decorator
public abstract class NotificationDecorator implements NotificationService {
    protected NotificationService notificationService;
    
    public NotificationDecorator(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @Override
    public void notify(String message) {
        notificationService.notify(message);
    }
}

@Service
public class EmailNotification extends NotificationDecorator {
    public EmailNotification(NotificationService notificationService) {
        super(notificationService);
    }
    
    @Override
    public void notify(String message) {
        super.notify(message);
        sendEmail(message);
    }
    
    private void sendEmail(String message) {
        // Email sending logic
    }
}

@Service
public class SMSNotification extends NotificationDecorator {
    public SMSNotification(NotificationService notificationService) {
        super(notificationService);
    }
    
    @Override
    public void notify(String message) {
        super.notify(message);
        sendSMS(message);
    }
    
    private void sendSMS(String message) {
        // SMS sending logic
    }
}