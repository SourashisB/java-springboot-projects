package main.java.com.example.repository;

import com.example.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository provides CRUD methods automatically
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}