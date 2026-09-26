package com.ljq.petagent.repository;

import com.ljq.petagent.entity.CartItem;
import com.ljq.petagent.entity.Dog;
import com.ljq.petagent.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProduct(Long cartId, Product product);

    Optional<CartItem> findByCartIdAndDog(Long cartId, Dog dog);

    List<CartItem> findByCartIdOrderByIdAsc(Long cartId);

    void deleteByCartId(Long cartId);
}
