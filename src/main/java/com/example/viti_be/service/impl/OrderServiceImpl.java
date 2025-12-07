package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.OrderRequest;
import com.example.viti_be.model.Order;
import com.example.viti_be.repository.OrderRepository;
import com.example.viti_be.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderRepository repo;
    @Override
    public Order getOrderById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Can not find Order by ID: " + id));
    }
    @Override
    public List<Order> getAllOrders(){
        return repo.findAll();
    }
    @Override
    public Order createOrder(OrderRequest request){
        return new Order();
    }
    @Override
    public Order confirmOrder(UUID orderId, UUID employeeId){
        return new Order();
    }
    @Override
    public void deleteOrder(UUID id){
        repo.deleteById(id);
    }
}
