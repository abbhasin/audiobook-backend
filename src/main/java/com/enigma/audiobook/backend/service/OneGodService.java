package com.enigma.audiobook.backend.service;

import com.enigma.audiobook.backend.aws.models.MPUAbortedReason;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.dao.*;
import com.enigma.audiobook.backend.models.*;
import com.enigma.audiobook.backend.models.requests.*;
import com.enigma.audiobook.backend.models.responses.*;
import com.enigma.audiobook.backend.utils.ContentUploadUtils;
import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private final CollectionConfigDao collectionConfigDao;
    private final ContentUploadUtils contentUploadUtils;

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


    public PostInitResponse initPosts(PostInitRequest postInitReq) {
        checkAuthorization(postInitReq.getPost().getFromUserId());
        if (postInitReq.getPost().getType() == null) {
            postInitReq.getPost().setType(PostType.TEXT);
        }
        String id = postsDao.generateId();
        UploadInitRes initRes = null;
        UploadInitReq uploadInitReq = postInitReq.getUploadInitReq();

        if (uploadInitReq != null) {
            if (postInitReq.getPost().getType() != PostType.TEXT) {
                String objectKeyFormat = null;
                switch (postInitReq.getPost().getType()) {
                    case VIDEO:
                        objectKeyFormat = getPostVideoUploadObjectKeyFormat(postInitReq.getPost().getPostId(),
                                postInitReq.getPost().getFromUserId());
                        break;
                    case AUDIO:
                        objectKeyFormat = getPostImageUploadObjectKeyFormat(postInitReq.getPost().getPostId(),
                                postInitReq.getPost().getFromUserId());
                        break;
                    case IMAGES:
                        objectKeyFormat = getPostAudioUploadObjectKeyFormat(postInitReq.getPost().getPostId(),
                                postInitReq.getPost().getFromUserId());
                        break;
                    case TEXT:
                        break;
                }


                initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat);
                if (!initRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
                    return new PostInitResponse(null, initRes);
                }

                List<String> urls =
                        initRes.getFileNameToUploadFileResponse()
                                .values()
                                .stream()
                                .map(uploadFileInitRes -> contentUploadUtils.getObjectUrl(uploadFileInitRes.getObjectKey()))
                                .toList();

                if (urls.isEmpty()) {
                    initRes.setRequestStatus(MPURequestStatus.ABORTED);
                    initRes.setAbortedReason(MPUAbortedReason.URLS_NOT_GENERATED);
                }
                switch (postInitReq.getPost().getType()) {
                    case VIDEO:
                        Preconditions.checkState(urls.size() == 1);
                        postInitReq.getPost().setVideoUrl(urls.get(0));
                        break;
                    case AUDIO:
                        Preconditions.checkState(urls.size() == 1);
                        postInitReq.getPost().setAudioUrl(urls.get(0));
                        break;
                    case IMAGES:
                        Preconditions.checkState(urls.size() <= 10);
                        postInitReq.getPost().setImagesUrl(urls);
                        break;
                    case TEXT:
                        break;
                }
            }
        }

        Post post = postsDao.initPost(postInitReq.getPost(), id);
        return new PostInitResponse(post, initRes);
    }


    public PostCompletionResponse postUploadUpdatePost(PostContentUploadReq postContentUploadReq) {
        checkAuthorization(postContentUploadReq.getPost().getFromUserId());
        Optional<Post> post = postsDao.getPost(postContentUploadReq.getPost().getPostId());
        Preconditions.checkState(post.isPresent());
        Preconditions.checkState(post.get().getType().equals(postContentUploadReq.getPost().getType()));

        String objectKey = null;
        String url = null;
        switch (post.get().getType()) {
            case VIDEO:
                Preconditions.checkState(postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() == 1);
                objectKey = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().get(0).getObjectKey();
                url = contentUploadUtils.getObjectUrl(objectKey);
                Preconditions.checkState(post.get().getVideoUrl().equals(url));
                break;
            case AUDIO:
                Preconditions.checkState(postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() == 1);
                objectKey = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().get(0).getObjectKey();
                url = contentUploadUtils.getObjectUrl(objectKey);
                Preconditions.checkState(post.get().getAudioUrl().equals(url));
                break;
            case IMAGES:
                int imagesSize = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size();
                Preconditions.checkState(imagesSize <= 10 && imagesSize == post.get().getImagesUrl().size());
                List<String> urls = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs()
                        .stream()
                        .map(UploadFileCompletionReq::getObjectKey)
                        .map(contentUploadUtils::getObjectUrl)
                        .sorted()
                        .toList();

                Preconditions.checkState(post.get().getImagesUrl().stream().sorted().equals(urls));
                break;
            case TEXT:
                break;
        }

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(postContentUploadReq.getUploadCompletionReq());

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new PostCompletionResponse(postContentUploadReq.getPost(), uploadCompletionRes);
        }

        Post completePost = postsDao.updatePostStatus(postContentUploadReq.getPost().getPostId(),
                ContentUploadStatus.RAW_UPLOADED);
        return new PostCompletionResponse(completePost, uploadCompletionRes);
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

    public CuratedFeedResponse getCuratedFeed(CuratedFeedRequest curatedFeedRequest) {
        String userId = curatedFeedRequest.getUserId();
        checkUserExists(userId);

        List<Following> followingsForUser = followingsDao.getFollowingsForUser(userId);

        List<Following> mandirFollowings =
                followingsForUser.stream()
                        .filter(f -> f.getFollowingType().equals(FollowingType.MANDIR))
                        .toList();

        Map<String, List<Post>> lastNMandirFollowingPosts =
                mandirFollowings.stream()
                        .collect(Collectors.toMap(Following::getFolloweeId,
                                f -> postsDao.getPosts(f.getFolloweeId(), PostAssociationType.MANDIR, 100)));

        Optional<String> scoredCollectionName = collectionConfigDao.getScoredCollectionName();

        List<String> scoredContentVideosPostIds =
                scoredCollectionName.map(s -> scoredContentDao.getScoredContentSorted(s, 1000, PostType.VIDEO)
                        .stream().map(ScoredContent::getPostId).toList()).orElse(Collections.emptyList());

        List<String> scoredContentAudioPostIds =
                scoredCollectionName.map(s -> scoredContentDao.getScoredContentSorted(s, 1000, PostType.AUDIO)
                        .stream().map(ScoredContent::getPostId).toList()).orElse(Collections.emptyList());

        List<String> newVideosPostIds = newPostsDao.getNewPostsByTypeNext(PostType.VIDEO, 100, Optional.empty())
                .stream().map(NewPost::getPostId).toList();

        List<String> newAudioPostIds = newPostsDao.getNewPostsByTypeNext(PostType.AUDIO, 100, Optional.empty())
                .stream().map(NewPost::getPostId).toList();

        Set<String> viewedPostIdsForUser = new HashSet<>(viewsDao.getViewsForUser(userId, 10000)
                .stream().map(View::getPostId).toList());

        /**
         * Feed Logic:
         */

        List<Post> curatedPosts = new ArrayList<>();
        Set<String> curatedPostIds = new HashSet<>();

        addCuratedPosts(lastNMandirFollowingPosts,
                scoredContentVideosPostIds,
                newVideosPostIds,
                scoredContentAudioPostIds,
                newAudioPostIds,
                viewedPostIdsForUser,
                curatedPosts,
                curatedPostIds);
        // TODO: We can paginate here over more entries from followed mandir, new posts and scored content

        // fallback to add to curated feed irrespective of viewed or not but not adding duplicated if already in
        // curated list
        if (curatedPosts.size() < 100) {
            addCuratedPosts(lastNMandirFollowingPosts,
                    scoredContentVideosPostIds,
                    newVideosPostIds,
                    scoredContentAudioPostIds,
                    newAudioPostIds,
                    Collections.emptySet(),
                    curatedPosts,
                    curatedPostIds);
        }

        CuratedFeedResponse curatedFeedResponse = new CuratedFeedResponse();
        curatedFeedResponse.setPosts(curatedPosts);
        return curatedFeedResponse;
    }

    private void addCuratedPosts(Map<String, List<Post>> lastNMandirFollowingPosts,
                                 List<String> scoredContentVideosPostIds,
                                 List<String> newVideosPostIds,
                                 List<String> scoredContentAudioPostIds,
                                 List<String> newAudioPostIds,
                                 Set<String> viewedPostIdsForUser,
                                 List<Post> curatedPosts,
                                 Set<String> curatedPostIds) {
        Integer countOfMandirPostPerIteration = 4;
        Integer countOfScoredVideoPostsPerIteration = 2;
        Integer countOfNewVideoPostsPerIteration = 2;
        Integer countOfScoredAudioPostsPerIteration = 2;
        Integer countOfNewAudioPostsPerIteration = 2;

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

        AtomicInteger scoredContentAudioPostIdsCount = new AtomicInteger(scoredContentAudioPostIds.size());
        AtomicInteger newAudioPostIdsCount = new AtomicInteger(newAudioPostIds.size());

        while (curatedPosts.size() < 100 &&
                (mandirPostsTotalCount.get() > 0 ||
                        scoredContentVideosPostIdsCount.get() > 0 ||
                        newVideosPostIdsCount.get() > 0)) {

            // mandir posts
            addMandirPosts(countOfMandirPostPerIteration,
                    mandirPostsTotalCount,
                    postEntriesByMandir,
                    postEntriesByMandirPtr,
                    postEntriesPtrByMandir,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);

            // scored content video posts
            addContent(countOfScoredVideoPostsPerIteration,
                    scoredContentVideosPostIdsCount,
                    scoredContentVideosPostIds,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);

            // scored content audio posts
            addContent(countOfScoredAudioPostsPerIteration,
                    scoredContentAudioPostIdsCount,
                    scoredContentAudioPostIds,
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

            // new audio
            addContent(countOfNewAudioPostsPerIteration,
                    newAudioPostIdsCount,
                    newAudioPostIds,
                    curatedPostIds,
                    viewedPostIdsForUser,
                    curatedPosts);
        }
    }

    /**
     * round robin over all following mandirs, adding posts if not viewed or already added to
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

    public void addFollowing(Following following) {
        followingsDao.upsertFollowing(following);
    }

    public void removeFollowing(Following following) {
        followingsDao.removeFollowing(following);
    }

    public List<Following> getFollowingsForUser(String userId) {
        return followingsDao.getFollowingsForUser(userId);
    }

    public void addViewForUser(View view) {
        viewsDao.upsertView(view);
    }

    private void checkUserExists(String userId) {
        Preconditions.checkState(userRegistrationDao.getUser(userId).isPresent());
    }

    private void checkAuthorization(String userId) {
        Optional<User> user = userRegistrationDao.getUser(userId);
        if (user.isEmpty() || StringUtils.isEmpty(user.get().getAuthUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "user is not authorized:" + userId);
        }
    }
}
