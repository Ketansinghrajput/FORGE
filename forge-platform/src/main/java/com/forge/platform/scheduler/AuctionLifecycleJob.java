package com.forge.platform.scheduler;

import com.forge.platform.service.AuctionManagerService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AuctionLifecycleJob implements Job {

    @Autowired
    private AuctionManagerService auctionManagerService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("⚡ Quartz: Running AuctionLifecycleJob...");
        try {
            auctionManagerService.startPendingAuctions();
            auctionManagerService.closeExpiredAuctions();
        } catch (Exception e) {
            log.error("❌ Quartz Job failed: {}", e.getMessage());
        }
    }
}