package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.dao.*;
import com.enigma.audiobook.backend.models.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CuratedFeedHandler implements Runnable {
    PostsDao postsDao;
    ViewsDao viewsDao;
    ScoredContentDao scoredContentDao;
    NewPostsDao newPostsDao;
    CollectionConfigDao collectionConfigDao;

    static final Integer MIN_VIEWS_THRESHOLD_FOR_SCORING = 2;
    static final Integer THIRTY_SEC = 30;
    static final Integer SIXTY_SEC = 60;

    static final Integer TWENTY_SEC = 20;
    static final Integer FORTY_SEC = 40;
    static final float SCORE_FACTOR = 1.41f;

    @Override
    public void run() {
        String scoredContentCollectionSuffix = generateSuffix();
        String collectionName = ScoredContentDao.getCollectionName(scoredContentCollectionSuffix);

        scoredContentDao.initCollectionAndIndexes(collectionName);

        addScoredContent(collectionName, PostType.VIDEO, SIXTY_SEC, THIRTY_SEC);
        addScoredContent(collectionName, PostType.AUDIO, FORTY_SEC, TWENTY_SEC);

        collectionConfigDao.updateScoredContentCollectionName(collectionName);
    }

    private void addScoredContent(String collectionName,
                                  PostType postType,
                                  int tierOneViewDurationThreshold,
                                  int tierTwoViewDuration) {
        List<Post> posts = postsDao.getPostsByType(postType, 10000, Optional.empty());

        posts.forEach(post -> {
            List<View> viewsForPost = viewsDao.getViewsForPostNext(post.getPostId(), 1000,
                    Optional.empty());
//            viewsForPost = viewsForPost
//                    .stream()
//                    .filter(view -> !view.getUserId().equals(post.getFromUserId()))
//                    .toList();

            if (viewsForPost.size() < MIN_VIEWS_THRESHOLD_FOR_SCORING) {
                NewPost newPost = new NewPost();
                newPost.setPostType(postType);
                newPost.setPostId(post.getPostId());
                newPostsDao.upsertNewPost(newPost);
                return;
            } else {
                newPostsDao.removeFromNewPosts(post.getPostId());
            }

            List<Integer> viewDurations = viewsForPost.stream()
                    .map(View::getViewDurationSec)
                    .toList();
            int totalViews = viewDurations.size();

            long numOfViewsGreaterThanTierOneThreshold = viewDurations
                    .stream()
                    .filter(duration -> duration > tierOneViewDurationThreshold)
                    .count();
            long numOfViewsGreaterThanTierTwoThreshold = viewDurations
                    .stream()
                    .filter(duration -> duration < tierOneViewDurationThreshold
                            && duration > tierTwoViewDuration)
                    .count();
            float tierOnePercent = ((float) numOfViewsGreaterThanTierOneThreshold / totalViews) * 100;
            float tierTwoPercent = ((float) numOfViewsGreaterThanTierTwoThreshold / totalViews) * 100;

            int score = (int) (tierTwoPercent * Math.pow(SCORE_FACTOR, 1) +
                    tierOnePercent * Math.pow(SCORE_FACTOR, 2));

            ScoredContent sc = new ScoredContent();
            sc.setScore(score);
            sc.setPostId(post.getPostId());
            sc.setPostType(postType);

            scoredContentDao.addScoredContent(collectionName, sc);
        });

    }

    private String generateSuffix() {
        return LocalDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST")))
                .format(DateTimeFormatter.ISO_DATE_TIME).toString();
    }
}
