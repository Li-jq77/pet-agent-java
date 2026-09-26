package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findFirstByUserIdOrderByIdAsc(Long userId);
}
