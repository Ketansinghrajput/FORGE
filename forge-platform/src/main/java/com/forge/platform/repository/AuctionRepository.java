package com.forge.platform.repository;

import com.forge.platform.entity.Auction;
import com.forge.platform.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatus(AuctionStatus status);
}