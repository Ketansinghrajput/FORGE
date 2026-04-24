package com.forge.platform.repository;

import com.forge.platform.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // 1. Ek auction ki saari bids descending order mein (latest first)
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    // 2. Specific user ne kis auction pe kitni bids lagayi hain
    List<Bid> findByBidderId(Long bidderId);

    // 3. Pro Level Query: Find the highest bid for an auction efficiently
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBidForAuction(@Param("auctionId") Long auctionId);
}