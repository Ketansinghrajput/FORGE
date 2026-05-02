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

    /**
     * 1. Ek auction ki saari bids descending order mein.
     * Note: 'auction.id' use kar rahe hain kyunki Bid entity mein 'Auction' object hai.
     */
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    void deleteByAuctionId(Long auctionId);

    /**
     * 2. Specific user ki bidding history.
     * Note: 'bidder.id' use hoga kyunki entity mein 'User bidder' relation hai.
     */
    List<Bid> findByBidderId(Long bidderId);

    /**
     * 3. Pro Level: Highest bid nikalne ke liye optimized query.
     * Barclays Pune ke round mein 'Limit 1' ya 'Top 1' ka use dikhana impressed karta hai.
     */
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBidForAuction(@Param("auctionId") Long auctionId);
}