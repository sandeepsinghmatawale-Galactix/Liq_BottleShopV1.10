package com.barinventory.admin.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BarSubscriptionScheduler {

    private final BarService barService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireSubscriptions() {
        int expiredCount = barService.expireDueSubscriptions();
        if (expiredCount > 0) {
            log.info("Expired {} bar subscriptions", expiredCount);
        }
    }
}
