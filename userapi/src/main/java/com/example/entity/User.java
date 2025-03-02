package main.java.com.example.entity;

import jakarta.persistence.*;
import lombok.*;

// Represents user table in DB
@Entity
@Data // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment ID
    private Long id;

    private String name;
    private String email;
}