package com.forge.platform.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QuartzJobScheduler {

    @Bean
    public JobDetail auctionLifecycleJobDetail() {
        return JobBuilder.newJob(AuctionLifecycleJob.class)
                .withIdentity("auctionLifecycleJob", "auction-group")
                .withDescription("Starts pending auctions and closes expired ones")
                .storeDurably()
                .requestRecovery()
                .build();
    }

    @Bean
    public Trigger auctionLifecycleTrigger(JobDetail auctionLifecycleJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(auctionLifecycleJobDetail)
                .withIdentity("auctionLifecycleTrigger", "auction-group")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}