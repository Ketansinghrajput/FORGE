package com.forge.platform.repository;

import com.forge.platform.entity.Auction;
import com.forge.platform.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(Long id);
    @Query("SELECT a FROM Auction a WHERE a.status = 'PLANNED' AND a.startTime <= :now")
    List<Auction> findPendingAuctions(@Param("now") LocalDateTime now);

    // Custom Query: Jo auctions CLOSE hone ke liye ready hain
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);

    // ✅ Enum based query: 'PLANNED' string ki jagah Enum use karna better hai
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.startTime <= :now")
    List<Auction> findByStatusAndStartTimeBefore(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);

    // ✅ Optimized Expired Query: Seedha status aur time check
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime <= :now")
    List<Auction> findByStatusAndEndTimeBefore(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);
}

