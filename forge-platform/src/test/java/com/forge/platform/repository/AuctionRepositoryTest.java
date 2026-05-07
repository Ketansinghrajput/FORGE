package com.forge.platform.repository;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AuctionRepositoryTest {

    @MockBean PasswordEncoder passwordEncoder;

    @Autowired AuctionRepository auctionRepository;
    @Autowired UserRepository userRepository;
    @Autowired BidRepository bidRepository;
    @Autowired WalletRepository walletRepository;

    private User seller;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        walletRepository.deleteAll();

        seller = new User();
        seller.setEmail("seller_" + System.nanoTime() + "@forge.com");
        seller.setPassword("encoded");
        seller.setFullName("Seller One");
        seller.setRole(UserRole.USER);
        seller = userRepository.save(seller);
    }

    private Auction buildAuction(String title, AuctionStatus status, LocalDateTime endTime) {
        Auction a = new Auction();
        a.setTitle(title);
        a.setDescription("desc");
        a.setStartingPrice(new BigDecimal("100.00"));
        a.setCurrentHighestBid(new BigDecimal("100.00"));
        a.setStartTime(LocalDateTime.now().minusHours(1));
        a.setEndTime(endTime);
        a.setStatus(status);
        a.setSeller(seller);
        return a;
    }

    @Test
    void saveAndFind_returnsAuction() {
        Auction saved = auctionRepository.save(
                buildAuction("Guitar", AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1)));

        Optional<Auction> found = auctionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Guitar");
    }

    @Test
    void findByStatusIn_returnsMatchingStatuses() {
        auctionRepository.save(buildAuction("A1", AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1)));
        auctionRepository.save(buildAuction("A2", AuctionStatus.PLANNED, LocalDateTime.now().plusDays(2)));
        auctionRepository.save(buildAuction("A3", AuctionStatus.CLOSED, LocalDateTime.now().minusDays(1)));

        var result = auctionRepository.findByStatusIn(
                List.of(AuctionStatus.ACTIVE, AuctionStatus.PLANNED),
                org.springframework.data.domain.Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2)
                .extracting(Auction::getStatus)
                .containsExactlyInAnyOrder(AuctionStatus.ACTIVE, AuctionStatus.PLANNED);
    }

    @Test
    void findByStatusAndEndTimeBefore_returnsExpiredOnly() {
        auctionRepository.save(buildAuction("Expired", AuctionStatus.ACTIVE, LocalDateTime.now().minusHours(1)));
        auctionRepository.save(buildAuction("Live",    AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1)));

        List<Auction> result = auctionRepository.findByStatusAndEndTimeBefore(
                AuctionStatus.ACTIVE, LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Expired");
    }

    @Test
    void findByIdWithLock_returnsAuction() {
        Auction saved = auctionRepository.save(
                buildAuction("Locked Item", AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1)));

        Optional<Auction> found = auctionRepository.findByIdWithLock(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Locked Item");
    }
}