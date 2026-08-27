package com.ecm.server.dto.request;

import com.ecm.server.common.CursorPageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SupplierFilterRequest extends CursorPageRequest {
    private String keyword;
    private String status;
}
