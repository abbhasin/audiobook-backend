package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.dao.ViewsDao;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.models.PostType;
import com.enigma.audiobook.backend.models.View;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CuratedFeedHandler implements Runnable {
    PostsDao postsDao;
    ViewsDao viewsDao;

    @Override
    public void run() {

        while (true) {
            List<Post> posts = postsDao.getPostsByType(PostType.VIDEO, 10000, Optional.empty());
            if (posts.isEmpty()) {
                break;
            }
            posts.forEach(post -> {
                String lastViewId = null;
                while (true) {
                    List<View> viewsForPost = viewsDao.getViewsForPostNext(post.getPostId(), 1000,
                            Optional.ofNullable(lastViewId));
                    if (viewsForPost.isEmpty()) {
                        break;
                    }
                    lastViewId = viewsForPost.get(viewsForPost.size() - 1).getId();

                    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();

                    List<Integer> viewDurations = viewsForPost.stream()
                            .map(View::getViewDurationSec)
                            .toList();
                    viewDurations.forEach(descriptiveStatistics::addValue);

                    Integer medianViewDuration = (int) descriptiveStatistics.getPercentile(50);
                    Integer totalViews = viewDurations.size();



                }

            });
        }


    }
}
