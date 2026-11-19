package com.taller.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(String orderId, String customerId, BigDecimal totalAmount,
                            int itemsCount, LocalDateTime processedAt) {}
