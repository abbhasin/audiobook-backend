package com.enigma.audiobook.backend.service;

import com.enigma.audiobook.backend.dao.*;
import com.enigma.audiobook.backend.models.*;
import com.enigma.audiobook.backend.models.requests.*;
import com.enigma.audiobook.backend.models.responses.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.*;

@Service
@Data
public class OneGodService {

    private final UserRegistrationDao userRegistrationDao;
    private final GodDao godDao;
    private final InfluencerDao influencerDao;
    private final PostsDao postsDao;
    private final DarshanDao darshanDao;
    private final CuratedDarshanDao curatedDarshanDao;
    private final MandirDao mandirDao;
    private final MandirAuthDao mandirAuthDao;
    private final FollowingsDao followingsDao;
    private final ScoredContentDao scoredContentDao;
    private final ViewsDao viewsDao;
    private final NewPostsDao newPostsDao;

    public User createUser() {
        return userRegistrationDao.registerNewUser();
    }

    public UserAssociationResponse associateAuthenticatedUser(UserRegistrationInfo userRegistrationInfo) {
        Optional<User> existingUser = userRegistrationDao.getUserWithAuthId(userRegistrationInfo.getAuthUserId());
        if (existingUser.isPresent()) {
            return new UserAssociationResponse(UserAssociationResponse.UserAssociationStatus.MAPPED_TO_EXISTING_USER,
                    existingUser.get());
        }

        userRegistrationDao.associateAuthenticatedUser(userRegistrationInfo.getUserId(),
                userRegistrationInfo.getAuthUserId(), userRegistrationInfo.getPhoneNumber());
        User user = userRegistrationDao.getUser(userRegistrationInfo.getUserId()).get();
        return new UserAssociationResponse(UserAssociationResponse.UserAssociationStatus.MAPPED_TO_GIVEN_USER,
                user);
    }

    public User getUser(String userId) {
        return userRegistrationDao.getUser(userId).orElse(null);
    }

    public GodInitResponse initGod(God god) {
        god.setImageUrl(null);
        god.setContentUploadStatus(ContentUploadStatus.PENDING);
        God godResponse = godDao.addGod(god);
        return new GodInitResponse(getGodImageUploadDirS3Url(godResponse.getGodId()), godResponse);
    }

    public God postUploadUpdateGod(GodImageUploadReq req) {
        return godDao.updateGod(req.getGodId(), req.getImageUrl(), ContentUploadStatus.PROCESSED);
    }

    public List<God> getGods(int limit) {
        return godDao.getGods(limit);
    }

    public List<God> getNextPageOfGods(int limit, String lastGodId) {
        return godDao.getGodsPaginated(limit, lastGodId);
    }

    public God getGod(String godId) {
        return godDao.getGod(godId).orElse(null);
    }


    public InfluencerInitResponse initInfluencer(Influencer influencer) {
        influencer.setImageUrl(null);
        influencer.setContentUploadStatus(ContentUploadStatus.PENDING);
        Influencer influencerResp = influencerDao.addInfluencer(influencer);
        return new InfluencerInitResponse(getInfluencerImageUploadDirS3Url(influencerResp.getUserId()), influencerResp);
    }


    public Influencer postUploadUpdateInfluencer(InfluencerImageUploadReq req) {
        return influencerDao.updateInfluencer(req.getUserId(), req.getImageUrl(), ContentUploadStatus.PROCESSED);
    }

    public List<Influencer> getInfluencers(int limit) {
        return influencerDao.getInfluencers(limit);
    }

    public List<Influencer> getNextPageOfInfluencers(int limit, String lastInfluencerId) {
        return influencerDao.getInfleuncersPaginated(limit, lastInfluencerId);
    }

    public Influencer getInfluencer(String userId) {
        return influencerDao.getInfleuncer(userId).orElse(null);
    }


    public PostInitResponse initPosts(Post posts) {
        checkAuthorization(posts.getFromUserId());
        if (posts.getType() == null) {
            posts.setType(PostType.TEXT);
        }
        Post post = postsDao.initPost(posts);
        return new PostInitResponse(getPostImageUploadDirS3Url(post.getPostId()),
                getPostVideoUploadDirS3Url(post.getPostId()),
                getPostAudioUploadDirS3Url(post.getPostId()),
                post);
    }


    public Post postUploadUpdatePost(PostContentUploadReq postContentUploadReq) {
        checkAuthorization(postContentUploadReq.getFromUserId());
        Post post = postsDao.updatePost(postContentUploadReq.getPostId(),
                postContentUploadReq.getContentUploadStatus(),
                postContentUploadReq.getPostType(),
                postContentUploadReq.getThumnailUrl(),
                postContentUploadReq.getVideoUrl(),
                postContentUploadReq.getImagesUrl(),
                postContentUploadReq.getAudioUrl());
        return post;
    }

    public Post getPost(String postId) {
        return postsDao.getPost(postId).orElse(null);
    }

    public List<Post> getPosts(int limit, String id, PostAssociationType associationType) {
        return postsDao.getPosts(id, associationType, limit);
    }

    public List<Post> getPostsPaginated(int limit, String id, PostAssociationType associationType,
                                        String lastPostId) {
        return postsDao.getPostsPaginated(id, associationType, limit, lastPostId);
    }

    public DarshanInitResponse initDarshan(Darshan darshan) {
        darshan.setVideoUploadStatus(ContentUploadStatus.PENDING);
        Darshan darshanRes = darshanDao.initDarshan(darshan);
        return new DarshanInitResponse(getDarshanVideoUploadDirS3Url(darshanRes.getDarshanId()),
                darshanRes);
    }


    public Darshan postUploadUpdateDarshan(DarshanContentUploadReq darshanContentUploadReq) {
        return darshanDao.updateDarshan(darshanContentUploadReq.getDarshanId(),
                darshanContentUploadReq.getThumbnailUrl(),
                darshanContentUploadReq.getVideoUrl(),
                darshanContentUploadReq.getStatus());
    }

    public List<Darshan> getCuratedDarshans() {
        Optional<CuratedDarshan> curatedDarshan =
                curatedDarshanDao.getLastNCuratedDarshan(1).stream().findFirst();

        Map<String, List<String>> curatedDarshans = curatedDarshan.get().getGodToDarshanIds();
        List<Darshan> darshans = new ArrayList<>();

        Map<String, Integer> godNameToCounter = curatedDarshans.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

        while (true) {
            for (Map.Entry<String, Integer> entry : godNameToCounter.entrySet()) {
                int counter = entry.getValue();
                if (counter > 0) {
                    List<String> darshansForGod = curatedDarshans.get(entry.getKey());
                    String darshanId = darshansForGod.get(darshansForGod.size() - counter);
                    Darshan darshan = darshanDao.getDarshan(darshanId).get();
                    darshans.add(darshan);
                    counter--;
                    entry.setValue(counter);
                }
            }
            boolean allZero = true;
            for (Map.Entry<String, Integer> entry : godNameToCounter.entrySet()) {
                allZero = allZero && entry.getValue() <= 0;
            }

            if (allZero) {
                break;
            }
        }

        return darshans;
    }

    public MandirInitResponse initMandir(Mandir mandir) {
        mandir.setImageUploadStatus(ContentUploadStatus.PENDING);
        Mandir mandirRes = mandirDao.initMandir(mandir);
        return new MandirInitResponse(getMandirImageUploadDirS3Url(mandirRes.getMandirId()),
                mandirRes);
    }


    public Mandir postUploadUpdateMandir(MandirContentUploadReq mandirContentUploadReq) {
        return mandirDao.updateMandir(mandirContentUploadReq.getMandirId(),
                mandirContentUploadReq.getImageUrl(),
                mandirContentUploadReq.getStatus());
    }

    public Mandir getMandir(String mandirId) {
        return mandirDao.getMandir(mandirId).orElse(null);
    }

    public List<Mandir> getMandirs(int limit) {
        return mandirDao.getMandirs(limit);
    }

    public List<Mandir> getMandirsPaginated(int limit, String lastMandirId) {
        return mandirDao.getMandirsPaginated(limit, lastMandirId);
    }

    public void addMandirAuth(String mandirId, String userId) {
        checkAuthorization(userId);
        mandirAuthDao.addMandirAuth(mandirId, userId, MandirAuth.ADMIN);
    }

    public List<Mandir> getAuthorizedMandirForUser(String userId) {
        checkAuthorization(userId);
        return mandirAuthDao.getMandirsWithAdminPermission(userId)
                .stream()
                .map(mandirId -> mandirDao.getMandir(mandirId).get())
                .collect(Collectors.toList());
    }

    public List<CuratedFeedResponse> getCuratedFeed(String userId) {
        List<Following> followingsForUser = followingsDao.getFollowingsForUser(userId);

        List<Following> mandirFollowings =
                followingsForUser.stream()
                        .filter(f -> f.getFollowerType().equals(FollowerType.MANDIR))
                        .toList();

        Map<String, List<Post>> lastNMandirFollowingPosts =
                mandirFollowings.stream()
                        .collect(Collectors.toMap(Following::getFolloweeId,
                                f -> postsDao.getPosts(f.getFolloweeId(), PostAssociationType.MANDIR, 100)));

        List<String> scoredContentVideosPostIds =
                scoredContentDao.getScoredContentSorted("", 1000, PostType.VIDEO)
                        .stream().map(ScoredContent::getPostId).toList();

        List<String> newVideosPostIds = newPostsDao.getNewPostsByTypeNext(PostType.VIDEO, 100, Optional.empty())
                .stream().map(NewPost::getPostId).toList();

        Set<String> viewedPostIdsForUser = new HashSet<>(viewsDao.getViewsForUser(userId, 10000)
                .stream().map(View::getPostId).toList());

        /**
         * Feed Logic:
         */
        List<Map.Entry<String, List<Post>>> postEntriesByMandir =
                new ArrayList<>(lastNMandirFollowingPosts.entrySet());
        AtomicInteger mandirPostsTotalCount =
                new AtomicInteger((int) lastNMandirFollowingPosts.values()
                        .stream()
                        .mapToLong(List::size)
                        .sum());
        AtomicInteger postEntriesByMandirPtr = new AtomicInteger(0);

        Map<String, Integer> postEntriesPtrByMandir =
                lastNMandirFollowingPosts.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));


        AtomicInteger scoredContentVideosPostIdsCount = new AtomicInteger(scoredContentVideosPostIds.size());
        AtomicInteger newVideosPostIdsCount = new AtomicInteger(newVideosPostIds.size());

        List<Post> curatedPosts = new ArrayList<>();
        Set<String> curatedPostIds = new HashSet<>();

        Integer countOfMandirPostPerIteration = 4;
        Integer countOfScoredVideoPostsPerIteration = 2;
        Integer countOfNewVideoPostsPerIteration = 2;
        Integer countOfScoredAudioPostsPerIteration = 2;

        while (curatedPostIds.size() < 100 &&
                (mandirPostsTotalCount.get() != 0 || scoredContentVideosPostIdsCount.get() != 0
                        || newVideosPostIdsCount.get() != 0)) {

            // mandir posts
            addMandirPosts(countOfMandirPostPerIteration,
                    mandirPostsTotalCount,
                    postEntriesByMandir,
                    postEntriesByMandirPtr,
                    postEntriesPtrByMandir,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);

            // scored content posts
            addContent(countOfScoredVideoPostsPerIteration,
                    scoredContentVideosPostIdsCount,
                    scoredContentVideosPostIds,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);
            // new videos
            addContent(countOfNewVideoPostsPerIteration,
                    newVideosPostIdsCount,
                    newVideosPostIds,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);
        }

        return new ArrayList<>();
    }

    /**
     * round robin over all following mandirs, adding posts if not viewed or already added toll
     * mandir posts count for this iteration.
     */
    private void addMandirPosts(Integer mandirPostsCount,
                                AtomicInteger mandirPostsTotalCount,
                                List<Map.Entry<String, List<Post>>> postEntriesByMandir,
                                AtomicInteger postEntriesByMandirPtr,
                                Map<String, Integer> postEntriesPtrByMandir,
                                Set<String> curatedPostIds,
                                Set<String> viewedPostIdsForUser,
                                List<Post> curatedPosts) {

        List<Post> mandirPosts = new ArrayList<>();
        while (mandirPostsTotalCount.get() > 0 && mandirPosts.size() < mandirPostsCount) {
            Map.Entry<String, List<Post>> entry =
                    postEntriesByMandir.get(postEntriesByMandirPtr.get());

            Integer entryValuePtr = postEntriesPtrByMandir.get(entry.getKey());

            while (entryValuePtr > 0) {
                Post post = entry.getValue().get(entry.getValue().size() - entryValuePtr);
                entryValuePtr--;
                postEntriesPtrByMandir.put(entry.getKey(), entryValuePtr);
                mandirPostsTotalCount.decrementAndGet();
                if (!viewedPostIdsForUser.contains(post.getPostId()) && !curatedPostIds.contains(post.getPostId())) {
                    // found mandir post to put
                    mandirPosts.add(post);
                    break;
                }
            }

            postEntriesByMandirPtr.set((postEntriesByMandirPtr.incrementAndGet()) % postEntriesByMandir.size());

        }

        if (!mandirPosts.isEmpty()) {
            mandirPosts.forEach(post -> curatedPostIds.add(post.getPostId()));
            curatedPosts.addAll(mandirPosts);
        }
    }

    public void addContent(Integer contentPostCount,
                           AtomicInteger scoredContentVideosPostIdsCount,
                           List<String> scoredContentVideosPostIds,
                           Set<String> curatedPostIds,
                           Set<String> viewedPostIdsForUser,
                           List<Post> curatedPosts) {
        List<String> scoredContentPostIds = new ArrayList<>();
        while (scoredContentPostIds.size() < contentPostCount && scoredContentVideosPostIdsCount.get() > 0) {
            String postId = scoredContentVideosPostIds.get(scoredContentVideosPostIds.size() -
                    scoredContentVideosPostIdsCount.get());
            scoredContentVideosPostIdsCount.decrementAndGet();

            if (!viewedPostIdsForUser.contains(postId) && !curatedPostIds.contains(postId)) {
                // found scored content post id to put
                scoredContentPostIds.add(postId);
            }
        }
        if (!scoredContentPostIds.isEmpty()) {
            curatedPostIds.addAll(scoredContentPostIds);
            scoredContentPostIds.forEach(scPostId ->
                    postsDao.getPost(scPostId).ifPresent(curatedPosts::add));
        }
    }


    private void checkAuthorization(String userId) {
        Optional<User> user = userRegistrationDao.getUser(userId);
        if (user.isEmpty() || StringUtils.isEmpty(user.get().getAuthUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "user is not authorized:" + userId);
        }
    }
}
