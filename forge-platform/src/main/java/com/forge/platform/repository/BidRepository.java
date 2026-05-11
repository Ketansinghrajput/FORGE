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


    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    void deleteByAuctionId(Long auctionId);


    List<Bid> findByBidderId(Long bidderId);

    long countByAuctionId(Long auctionId);


    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBidForAuction(@Param("auctionId") Long auctionId);
}