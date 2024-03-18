package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.dao.ScoredContentDao;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CuratedFeedCleanup implements Runnable {
    private static final int MAX_SCORED_CONTENT_COLLECTIONS = 4;
    ScoredContentDao scoredContentDao;

    @Override
    public void run() {
        List<String> scoredContentCollections = scoredContentDao.listCollections();
        scoredContentCollections = new ArrayList<>(scoredContentCollections.stream()
                .filter(name -> name.startsWith("scoredContent"))
                .sorted()
                .toList());

        if (scoredContentCollections.size() > MAX_SCORED_CONTENT_COLLECTIONS) {
            for (int i = 0; i < scoredContentCollections.size() - MAX_SCORED_CONTENT_COLLECTIONS; i++) {
                scoredContentCollections.remove(0);
            }
        }

        scoredContentDao.dropCollections(scoredContentCollections);

    }
}
