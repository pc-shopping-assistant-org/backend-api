package com.ecm.server.dto.request;

import com.ecm.server.common.CursorPageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InvoiceFilterRequest extends CursorPageRequest {
    /** Order UUID, invoice prefix, customer name or customer email. */
    private String keyword;
    private Instant fromDate;
    private Instant toDate;
}
