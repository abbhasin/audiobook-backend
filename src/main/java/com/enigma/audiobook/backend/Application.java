package com.enigma.audiobook.backend;

import com.enigma.audiobook.backend.jobs.ContentEncodingScheduler;
import com.enigma.audiobook.backend.jobs.CuratedDarshanHandler;
import com.enigma.audiobook.backend.jobs.CuratedFeedCleanup;
import com.enigma.audiobook.backend.jobs.CuratedFeedHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NamedBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Application {
    @Autowired
    ContentEncodingScheduler contentEncodingScheduler;
    @Autowired
    CuratedDarshanHandler curatedDarshanHandler;
    @Autowired
    CuratedFeedHandler curatedFeedHandler;
    @Autowired
    CuratedFeedCleanup curatedFeedCleanup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarted() {
        contentEncodingScheduler.start();
    }

    // every day at 10pm
    @Scheduled(cron = "0 50 9 * * *", scheduler = "appJobsScheduler")
    public void runCuratedDarshans() {
        log.info("running curated darshan");
        curatedDarshanHandler.run();
    }

    // every day at 10pm
//    @Scheduled(cron = "0 0 22 * * ?", scheduler = "appJobsScheduler")
    public void runCuratedFeed() {
        curatedFeedHandler.run();
    }

//    @Scheduled(cron = "0 0 22 * * ?", scheduler = "appJobsScheduler")
    public void runCuratedFeedCleanup() {
        curatedFeedCleanup.run();
    }

}
