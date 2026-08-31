package com.ecm.server.service;

import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.request.UpdateCartItemRequest;
import com.ecm.server.common.StatusCode;
import com.ecm.server.model.Cart;
import com.ecm.server.model.CartItem;
import com.ecm.server.model.CartItemId;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Product;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.CartRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.service.impl.CartServiceImpl;
import com.ecm.server.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CartServiceImpl service;

    @Test
    void addToCartLocksCartBeforeReadingCurrentVariantStock() {
        UUID accountId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        ProductVariant variant = ProductVariant.builder()
                .id(variantId)
                .sku("SKU-1")
                .status("ACTIVE")
                .listPrice(100L)
                .quantity(2)
                .product(Product.builder().status("ACTIVE").build())
                .build();
        Cart cart = Cart.builder()
                .id(cartId)
                .customer(customer)
                .items(new LinkedHashSet<>())
                .build();

        when(cartRepository.findActiveByAccountIdForUpdate(accountId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findActiveByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addToCart(accountId, AddToCartRequest.builder()
                .productVariantId(variantId)
                .quantity(1)
                .build());

        InOrder lockOrder = inOrder(cartRepository, productVariantRepository);
        lockOrder.verify(cartRepository).findActiveByAccountIdForUpdate(accountId);
        lockOrder.verify(productVariantRepository).findActiveByIdForUpdate(variantId);
        assertEquals(1, cart.getItems().iterator().next().getQuantity());
    }

    @Test
    void updateCartItemUsesLockedVariantForStockCheck() {
        UUID accountId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        ProductVariant variant = ProductVariant.builder()
                .id(variantId)
                .sku("SKU-1")
                .status("ACTIVE")
                .listPrice(100L)
                .quantity(3)
                .product(Product.builder().status("ACTIVE").build())
                .build();
        CartItem item = CartItem.builder()
                .id(new CartItemId(cartId, variantId))
                .variant(variant)
                .quantity(1)
                .build();
        Cart cart = Cart.builder()
                .id(cartId)
                .customer(customer)
                .items(new LinkedHashSet<>())
                .build();
        item.setCart(cart);
        cart.getItems().add(item);

        when(cartRepository.findActiveByAccountIdForUpdate(accountId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findActiveByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateCartItem(accountId, variantId, UpdateCartItemRequest.builder().quantity(3).build());

        verify(productVariantRepository).findActiveByIdForUpdate(variantId);
        assertEquals(3, item.getQuantity());
    }

    @Test
    void activeCartCreationConflictIsRejectedWithoutQueryingAbortedTransaction() {
        UUID accountId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();

        when(cartRepository.findActiveByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());
        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
        when(cartRepository.saveAndFlush(any(Cart.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate active cart"));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.addToCart(accountId, AddToCartRequest.builder()
                        .productVariantId(variantId)
                        .quantity(1)
                        .build()));

        assertEquals(StatusCode.CONFLICT, exception.getStatusCode());
        verify(cartRepository).saveAndFlush(any(Cart.class));
    }
}
