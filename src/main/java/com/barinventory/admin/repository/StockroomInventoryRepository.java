package com.barinventory.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Product;
import com.barinventory.admin.entity.StockroomInventory;

import jakarta.transaction.Transactional;

@Repository
public interface StockroomInventoryRepository extends JpaRepository<StockroomInventory, Long> {

    List<StockroomInventory> findBySession_SessionId(Long sessionId);

    //Optional<StockroomInventory> findBySession_SessionIdAndProduct_Id(Long sessionId, Long productId);

    @Modifying
    @Transactional
    void deleteBySession_SessionId(Long sessionId);

  //  @Query("SELECT s FROM StockroomInventory s WHERE s.session.sessionId = :sessionId")
   // List<StockroomInventory> findBySession_SessionIdWithProduct(@Param("sessionId") Long sessionId);

    long countBySession_SessionId(Long sessionId);

    Optional<StockroomInventory> findBySessionAndProduct(InventorySession session, Product product);
    
    // Find all stockroom entries by session
    // --------------------------
    List<StockroomInventory> findBySessionSessionId(Long sessionId);

    // --------------------------
    // Delete all stockroom entries by session
    // --------------------------
    @Modifying
    @Transactional
    void deleteBySessionSessionId(Long sessionId);
    
    long countBySessionSessionId(Long sessionId);

    // --------------------------
    // Optional: find by session entity
    // --------------------------
    List<StockroomInventory> findBySession(InventorySession session);
    
    Optional<StockroomInventory> findBySession_SessionIdAndProduct_ProductId(Long sessionId, Long productId);
    
    //@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockroomInventory s WHERE s.session.sessionId = :sessionId AND s.product.productId = :productId")
    //Integer sumQuantityBySessionAndProduct(@Param("sessionId") Long sessionId, @Param("productId") Long productId);
}