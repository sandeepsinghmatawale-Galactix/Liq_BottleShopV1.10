package com.barinventory.customer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.brands.entity.BrandSize;
import com.barinventory.brands.repository.BrandSizeRepository;
import com.barinventory.customer.dto.*;
import com.barinventory.customer.entity.CartItem;
import com.barinventory.customer.entity.ConsumptionLog;
import com.barinventory.customer.entity.Customer;
import com.barinventory.customer.entity.CustomerOrder;
import com.barinventory.customer.entity.OrderItem;
import com.barinventory.customer.repository.CartItemRepository;
import com.barinventory.customer.repository.ConsumptionLogRepository;
import com.barinventory.customer.repository.CustomerOrderRepository;
import com.barinventory.customer.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository orderRepository;
    private final ConsumptionLogRepository consumptionRepository;
    private final BrandSizeRepository brandSizeRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public Customer register(CustomerRegistrationDTO dto) {
        if (customerRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email already exists");
        if (customerRepository.existsByPhone(dto.getPhone()))
            throw new RuntimeException("Phone already exists");
        
        Customer customer = Customer.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .password(passwordEncoder.encode(dto.getPassword()))
            .dateOfBirth(dto.getDateOfBirth())
            .gender(dto.getGender())
            .weight(dto.getWeight())
            .active(true)
            .build();
        
        return customerRepository.save(customer);
    }
    
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCart(Long customerId) {
        return cartItemRepository.findByCustomerId(customerId).stream()
            .map(this::enrichCartItem)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void addToCart(Long customerId, Long brandSizeId, Integer quantity) {
        BrandSize size = brandSizeRepository.findById(brandSizeId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        cartItemRepository.findByCustomerIdAndBrandSizeId(customerId, brandSizeId)
            .ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + quantity),
                () -> {
                    Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
                    CartItem item = CartItem.builder()
                        .customer(customer)
                        .brandSizeId(brandSizeId)
                        .quantity(quantity)
                        .price(size.getSellingPrice())
                        .build();
                    cartItemRepository.save(item);
                }
            );
    }
    
    @Transactional
    public void updateCartItem(Long customerId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));
        if (!item.getCustomer().getId().equals(customerId))
            throw new RuntimeException("Unauthorized");
        
        if (quantity <= 0) cartItemRepository.delete(item);
        else item.setQuantity(quantity);
    }
    
    @Transactional
    public void removeFromCart(Long customerId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));
        if (!item.getCustomer().getId().equals(customerId))
            throw new RuntimeException("Unauthorized");
        cartItemRepository.delete(item);
    }
    
    @Transactional
    public CustomerOrder checkout(Long customerId) {
        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customerId);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerOrder order = CustomerOrder.builder()
            .customer(customer)
            .orderDate(LocalDateTime.now())
            .totalAmount(BigDecimal.ZERO)
            .status(CustomerOrder.OrderStatus.CONFIRMED)
            .build();
        
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            BrandSize size = brandSizeRepository.findById(cartItem.getBrandSizeId()).orElse(null);
            if (size == null) continue;
            
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .brandSizeId(size.getId())
                .brandName(size.getBrand().getBrandName())
                .sizeLabel(size.getSizeLabel())
                .volumeMl(size.getVolumeMl())
                .abvPercent(size.getAbvPercent())
                .quantity(cartItem.getQuantity())
                .price(size.getSellingPrice())
                .mrp(size.getMrp())
                .build();
            
            order.getItems().add(orderItem);
            total = total.add(size.getSellingPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        
        order.setTotalAmount(total);
        CustomerOrder saved = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public List<CustomerOrder> getOrderHistory(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }
    
    @Transactional
    public void logConsumption(Long customerId, ConsumptionLogDTO dto) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        double bac = calculateBAC(dto.getVolumeMl(), dto.getAbvPercent(), 
                                   customer.getWeight(), customer.getGender());
        
        ConsumptionLog log = ConsumptionLog.builder()
            .customer(customer)
            .consumptionTime(LocalDateTime.now())
            .brandName(dto.getBrandName())
            .sizeLabel(dto.getSizeLabel())
            .volumeMl(dto.getVolumeMl())
            .abvPercent(dto.getAbvPercent())
            .unitsConsumed(dto.getUnitsConsumed())
            .estimatedBac(bac)
            .build();
        
        consumptionRepository.save(log);
    }
    
    @Transactional(readOnly = true)
    public List<ConsumptionLog> getConsumptionHistory(Long customerId, LocalDateTime start, LocalDateTime end) {
        return consumptionRepository.findByCustomerIdAndConsumptionTimeBetween(customerId, start, end);
    }
    
    @Transactional(readOnly = true)
    public HealthStatsDTO getHealthStats(Long customerId, LocalDateTime start, LocalDateTime end) {
        Integer totalUnits = consumptionRepository.getTotalUnitsConsumed(customerId, start, end);
        Double totalSpent = orderRepository.getTotalSpent(customerId, start, end);
        
        return HealthStatsDTO.builder()
            .totalUnitsConsumed(totalUnits != null ? totalUnits : 0)
            .totalSpent(totalSpent != null ? BigDecimal.valueOf(totalSpent) : BigDecimal.ZERO)
            .periodStart(start)
            .periodEnd(end)
            .build();
    }
    
    private CartItemDTO enrichCartItem(CartItem item) {
        BrandSize size = brandSizeRepository.findById(item.getBrandSizeId()).orElse(null);
        return CartItemDTO.builder()
            .id(item.getId())
            .brandSizeId(item.getBrandSizeId())
            .brandName(size != null ? size.getBrand().getBrandName() : "")
            .sizeLabel(size != null ? size.getSizeLabel() : "")
            .quantity(item.getQuantity())
            .price(item.getPrice())
            .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .build();
    }
    
    private double calculateBAC(Integer volumeMl, Double abvPercent, Double weight, Customer.Gender gender) {
        if (volumeMl == null || abvPercent == null || weight == null) return 0.0;
        
        double alcoholGrams = (volumeMl * abvPercent / 100.0) * 0.789;
        double r = gender == Customer.Gender.FEMALE ? 0.55 : 0.68;
        return (alcoholGrams / (weight * r * 10)) * 100;
    }
}