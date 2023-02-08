package com.example.tgbot.Model.Repository;

import com.example.tgbot.Model.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
