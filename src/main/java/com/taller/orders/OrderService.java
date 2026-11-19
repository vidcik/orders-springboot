package com.taller.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final JdbcTemplate jdbc;

    public OrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public OrderResponse create(OrderRequest req) {
        List<OrderRequest.Item> items = req.items();

        BigDecimal total = BigDecimal.ZERO;
        int itemsCount = 0;
        for (OrderRequest.Item it : items) {
            total = total.add(BigDecimal.valueOf(it.price()).multiply(BigDecimal.valueOf(it.quantity())));
            itemsCount += it.quantity();
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        String orderId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        jdbc.update(
            "INSERT INTO orders (order_id, customer_id, total_amount, items_count, created_at) VALUES (?,?,?,?,?)",
            orderId, req.customerId(), total, itemsCount, java.sql.Timestamp.valueOf(now));

        jdbc.batchUpdate(
            "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?,?,?,?)",
            items, items.size(), (ps, it) -> {
                ps.setString(1, orderId);
                ps.setString(2, it.productId());
                ps.setInt(3, it.quantity());
                ps.setBigDecimal(4, BigDecimal.valueOf(it.price()));
            });

        return new OrderResponse(orderId, req.customerId(), total, itemsCount, now);
    }
}
