package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.OrderItemRequest;
import com.example.viti_be.dto.request.OrderRequest;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.OrderService;
import com.example.viti_be.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderRepository repo;

    @Autowired
    UserRepository userRepo;
    @Autowired
    CustomerRepository customerRepo;
    @Autowired
    PromotionRepository promotionRepo;
    @Autowired
    ProductRepository productRepo;

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
        Order order = new Order();

        User employee = userRepo.findByIdAndIsDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found by ID: " + request.getEmployeeId()));
//        Customer customer = customerRepo.findByIdAndIsDeletedFalse(request.getEmployeeId())
//                .orElseThrow(() -> new RuntimeException("Customer not found by ID: " + request.getCustomerId()));
        Promotion promotion = null;
        if (request.getPromotionId() != null) {
            promotion = promotionRepo.findById(request.getPromotionId())
                    .orElseThrow(() -> new RuntimeException("Promotion not found with ID: " + request.getPromotionId()));
        }

        order.setEmployee(employee);
//        order.setCustomer(customer);
        order.setPromotion(promotion);

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;
        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepo.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemRequest.getProductId()));

            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for product: " + product.getName());
            }

            // [OPTIONAL] Kiểm tra tồn kho

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setDiscount(itemRequest.getDiscount() != null ? itemRequest.getDiscount() : 0.0);

            //TODO:
            // Tính toán giá trị dòng hàng (đơn giá * số lượng * (1 - discount))
            double itemPrice = 0;
            double finalItemPrice = itemPrice * (1 - orderItem.getDiscount());
            orderItem.setSubtotal(finalItemPrice);

            totalAmount += finalItemPrice;
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        return repo.save(order);
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
