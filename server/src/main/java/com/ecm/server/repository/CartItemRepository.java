package com.ecm.server.repository;

import com.ecm.server.model.CartItem;
import com.ecm.server.model.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
}
