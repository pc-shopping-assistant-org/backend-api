package com.ecm.server.service;

import com.ecm.server.dto.request.CreateOrderRequest;
import com.ecm.server.dto.request.OrderItemRequest;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.model.Cart;
import com.ecm.server.model.CartItem;
import com.ecm.server.model.CartItemId;
import com.ecm.server.model.Category;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.model.Discount;
import com.ecm.server.model.Order;
import com.ecm.server.model.OrderItem;
import com.ecm.server.model.Payment;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.model.Product;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.model.ShippingMethod;
import com.ecm.server.repository.CartRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.DiscountCategoryRepository;
import com.ecm.server.repository.DiscountRepository;
import com.ecm.server.repository.DiscountVariantRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.repository.ShippingMethodRepository;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerAddressRepository customerAddressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private DiscountVariantRepository discountVariantRepository;
    @Mock private DiscountCategoryRepository discountCategoryRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private ShippingMethodRepository shippingMethodRepository;
    @Mock private OrderMapper orderMapper;

    @InjectMocks private OrderServiceImpl service;

    @Test
    void checkoutStoresOrderVoucherOnlyInOrderDiscountSnapshot() {
        UUID accountId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        ProductVariant variant = ProductVariant.builder()
                .id(variantId)
                .sku("SKU-1")
                .status("ACTIVE")
                .listPrice(20_000_000L)
                .quantity(2)
                .product(Product.builder().status("ACTIVE").build())
                .build();
        Cart cart = Cart.builder().id(cartId).customer(customer).items(new LinkedHashSet<>()).build();
        cart.getItems().add(CartItem.builder()
                .id(new CartItemId(cartId, variantId))
                .cart(cart)
                .variant(variant)
                .quantity(1)
                .build());

        Instant now = Instant.now();
        Discount itemPromotion = Discount.builder()
                .id(UUID.randomUUID()).discountType("PERCENT").value(10)
                .applicationScope("ALL_ITEMS").status("ACTIVE")
                .startAt(now.minusSeconds(60)).endAt(now.plusSeconds(60)).build();
        Discount orderVoucher = Discount.builder()
                .id(UUID.randomUUID()).code("SAVE500K").discountType("FIXED").value(500_000)
                .applicationScope("ORDER").status("ACTIVE")
                .minOrderAmount(0L).startAt(now.minusSeconds(60)).endAt(now.plusSeconds(60)).build();

        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
        when(cartRepository.findActiveByAccountIdForUpdate(accountId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findActiveByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
        when(discountRepository.findActiveAutomatic(any(Instant.class))).thenReturn(List.of(itemPromotion));
        when(discountRepository.findByCodeIgnoreCase("SAVE500K")).thenReturn(Optional.of(orderVoucher));
        when(shippingMethodRepository.findByCodeIgnoreCaseAndStatus("STANDARD", "ACTIVE"))
                .thenReturn(Optional.of(ShippingMethod.builder().code("STANDARD").status("ACTIVE").build()));
        when(paymentMethodRepository.findByCodeIgnoreCaseAndStatus("COD", "ACTIVE"))
                .thenReturn(Optional.of(PaymentMethod.builder().code("COD").status("ACTIVE").build()));
        when(customerAddressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId))
                .thenReturn(Optional.of(CustomerAddress.builder()
                        .customer(customer).recipientName("Recipient").phone("0912345678")
                        .addressLine("1 Main Street").isDefault(true).build()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toDetailResponse(any(Order.class))).thenReturn(OrderDetailResponse.builder().build());

        service.createOrder(accountId, CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder().productVariantId(variantId).quantity(1).build()))
                .discountCode("SAVE500K")
                .paymentMethod("COD")
                .build());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(18_000_000L, saved.getSubtotalAmount());
        assertEquals(500_000L, saved.getDiscountAmount());
        assertEquals(17_500_000L, saved.getTotalAmount());
        assertEquals(saved.getSubtotalAmount() - saved.getDiscountAmount() + saved.getShippingFee(),
                saved.getTotalAmount());
    }

    @Test
    void customerCanSearchOwnOrderByInvoicePrefix() {
        UUID accountId = UUID.randomUUID();
        Order matching = Order.builder()
                .id(UUID.fromString("30000000-0000-0000-0000-000000000001"))
                .customer(Customer.builder().accountId(accountId).build())
                .status(OrderServiceImpl.STATUS_COMPLETED)
                .build();
        Order other = Order.builder()
                .id(UUID.fromString("40000000-0000-0000-0000-000000000001"))
                .customer(Customer.builder().accountId(accountId).build())
                .status(OrderServiceImpl.STATUS_COMPLETED)
                .build();

        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(
                Customer.builder().accountId(accountId).build()));
        when(orderRepository.findCustomerOrdersByIdentifierInitial(any(UUID.class), any(String.class), any(), any()))
                .thenReturn(List.of(matching));
        when(orderItemRepository.findByOrderIdWithDetails(any(UUID.class))).thenReturn(List.of());
        when(paymentRepository.findByOrderId(any(UUID.class))).thenReturn(List.of());
        when(orderMapper.toDetailResponse(any(Order.class))).thenAnswer(invocation ->
                OrderDetailResponse.builder().id(((Order) invocation.getArgument(0)).getId()).build());

        var response = service.getMyOrders(accountId, OrderFilterRequest.builder()
                .keyword("INV-30000000")
                .limit(10)
                .build());

        assertEquals(1, response.getItems().size());
        assertEquals(matching.getId(), response.getItems().getFirst().getId());
    }

    @Test
    void checkoutRejectsCartContainingUnavailableVariantInsteadOfDroppingIt() {
        UUID accountId = UUID.randomUUID();
        UUID activeVariantId = UUID.randomUUID();
        UUID unavailableVariantId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        ProductVariant activeVariant = ProductVariant.builder()
                .id(activeVariantId)
                .sku("ACTIVE-SKU")
                .status("ACTIVE")
                .listPrice(100L)
                .quantity(2)
                .product(com.ecm.server.model.Product.builder().status("ACTIVE").build())
                .build();
        ProductVariant unavailableVariant = ProductVariant.builder()
                .id(unavailableVariantId)
                .sku("HIDDEN-SKU")
                .status("INACTIVE")
                .listPrice(200L)
                .quantity(2)
                .product(com.ecm.server.model.Product.builder().status("ACTIVE").build())
                .build();
        Cart cart = Cart.builder().id(cartId).customer(customer).items(new LinkedHashSet<>()).build();
        cart.getItems().add(CartItem.builder()
                .id(new CartItemId(cartId, activeVariantId))
                .cart(cart)
                .variant(activeVariant)
                .quantity(1)
                .build());
        cart.getItems().add(CartItem.builder()
                .id(new CartItemId(cartId, unavailableVariantId))
                .cart(cart)
                .variant(unavailableVariant)
                .quantity(1)
                .build());

        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
        when(cartRepository.findActiveByAccountIdForUpdate(accountId)).thenReturn(Optional.of(cart));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.createOrder(accountId, CreateOrderRequest.builder()
                        .items(List.of(OrderItemRequest.builder()
                                .productVariantId(activeVariantId)
                                .quantity(1)
                                .build()))
                        .paymentMethod("COD")
                        .build()));

        assertEquals(com.ecm.server.common.StatusCode.BAD_REQUEST, exception.getStatusCode());
    }
}
