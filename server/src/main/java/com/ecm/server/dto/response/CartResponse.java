package com.ecm.server.dto.response;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    @Builder.Default
    private List<CartItemResponse> items = Collections.emptyList();

    private Integer totalItems;

    private Long subtotalAmount;
}
