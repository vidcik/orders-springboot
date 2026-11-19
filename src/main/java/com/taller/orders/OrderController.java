package com.taller.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderRequest req) {
        if (req.customerId() == null || req.items() == null || req.items().isEmpty()) {
            return ResponseEntity.badRequest().body("customerId e items son obligatorios");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }
}
