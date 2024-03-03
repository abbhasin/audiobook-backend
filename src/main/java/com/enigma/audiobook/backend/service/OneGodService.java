package com.enigma.audiobook.backend.service;

import com.enigma.audiobook.backend.aws.models.MPUAbortedReason;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.dao.*;
import com.enigma.audiobook.backend.models.*;
import com.enigma.audiobook.backend.models.requests.*;
import com.enigma.audiobook.backend.models.responses.*;
import com.enigma.audiobook.backend.proxies.FirebaseClient;
import com.enigma.audiobook.backend.utils.ContentUploadUtils;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OneGodService {

    private final UserRegistrationDao userRegistrationDao;
    private final GodDao godDao;
    private final InfluencerDao influencerDao;
    private final PostsDao postsDao;
    private final DarshanDao darshanDao;
    private final CuratedDarshanDao curatedDarshanDao;
    private final MandirDao mandirDao;
    private final MandirAuthDao mandirAuthDao;
    private final UserAuthDao userAuthDao;
    private final FollowingsDao followingsDao;
    private final ScoredContentDao scoredContentDao;
    private final ViewsDao viewsDao;
    private final NewPostsDao newPostsDao;
    private final CollectionConfigDao collectionConfigDao;
    private final ContentUploadUtils contentUploadUtils;
    private final FirebaseClient firebaseClient;

    public User createUser() {
        return userRegistrationDao.registerNewUser();
    }

    public UserAssociationResponse associateAuthenticatedUser(UserRegistrationInfo userRegistrationInfo) {
        Optional<User> existingUser = userRegistrationDao.getUserWithAuthId(userRegistrationInfo.getAuthUserId());
        if (existingUser.isPresent()) {
            userRegistrationDao.updateMetadata(userRegistrationInfo.getUserId(),
                    userRegistrationInfo.getAuthUserId());
            return new UserAssociationResponse(UserAssociationResponse.UserAssociationStatus.MAPPED_TO_EXISTING_USER,
                    existingUser.get());
        }

        Optional<FirebaseClient.FirebaseUserInfo> firebaseUserInfo =
                firebaseClient.getUserInfo(userRegistrationInfo.getAuthUserId());

        if (firebaseUserInfo.isEmpty()) {
            return new UserAssociationResponse(UserAssociationResponse.UserAssociationStatus.FAILED_AUTH_USER_NOT_FOUND,
                    null);
        }

        userRegistrationDao.associateAuthenticatedUser(userRegistrationInfo.getUserId(),
                userRegistrationInfo.getAuthUserId(), firebaseUserInfo.get().getPhoneNum());

        User user = userRegistrationDao.getUser(userRegistrationInfo.getUserId()).get();
        return new UserAssociationResponse(UserAssociationResponse.UserAssociationStatus.MAPPED_TO_GIVEN_USER,
                user);
    }

    public User getUser(String userId) {
        return userRegistrationDao.getUser(userId).orElse(null);
    }

    public GodInitResponse initGod(GodInitRequest godInitRequest) {
        String id = godDao.generateId();

        UploadInitReq uploadInitReq = godInitRequest.getUploadInitReq();

        Preconditions.checkState(uploadInitReq != null);
        String objectKeyFormat = getGodImageUploadObjectKeyFormat(id);

        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() <= 10);

        UploadInitRes initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat,
                ContentUploadUtils.ContentTypeByExtension.IMAGE);
        if (!initRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new GodInitResponse(null, initRes);
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
        Preconditions.checkState(urls.size() <= 10);
        godInitRequest.getGod().setImageUrl(urls);

        godInitRequest.getGod().setContentUploadStatus(ContentUploadStatus.PENDING);
        God godResponse = godDao.addGod(godInitRequest.getGod(), id);
        return new GodInitResponse(godResponse, initRes);
    }

    public GodCompletionResponse postUploadUpdateGod(GodContentUploadReq godContentUploadReq) {
        Optional<God> god = godDao.getGod(godContentUploadReq.getGod().getGodId());
        Preconditions.checkState(god.isPresent());
        Preconditions.checkState(godContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() <= 10);

        List<String> urls =
                godContentUploadReq.getUploadCompletionReq()
                        .getUploadFileCompletionReqs()
                        .stream()
                        .map(uploadFileCompletionReq -> contentUploadUtils.getObjectUrl(uploadFileCompletionReq.getObjectKey()))
                        .sorted()
                        .toList();
        List<String> actualUrls = god.get().getImageUrl().stream().sorted().toList();
        Preconditions.checkState(actualUrls.equals(urls));

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(godContentUploadReq.getUploadCompletionReq(),
                        ContentUploadUtils.ContentTypeByExtension.IMAGE);

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new GodCompletionResponse(godContentUploadReq.getGod(),
                    uploadCompletionRes);
        }
        God godCompleted = godDao.updateGodStatus(godContentUploadReq.getGod().getGodId(),
                ContentUploadStatus.PROCESSED);
        return new GodCompletionResponse(godCompleted, uploadCompletionRes);
    }

    public List<God> getGods(int limit) {
        return godDao.getGods(limit);
    }

    public List<God> getNextPageOfGods(int limit, String lastGodId) {
        return godDao.getGodsPaginated(limit, lastGodId);
    }

    public List<GodForUser> getGodsForUser(int limit, String userId) {
        Set<String> followedGodIds = getFolloweeIdsForUser(userId, FollowingType.GOD);

        return godDao.getGods(limit)
                .stream()
                .map(god -> {
                    GodForUser godForUser = new GodForUser();
                    godForUser.setGod(god);
                    godForUser.setFollowed(followedGodIds.contains(god.getGodId()));
                    return godForUser;
                }).toList();
    }

    public List<GodForUser> getNextPageOfGodsForUser(int limit, String lastGodId, String userId) {
        Set<String> followedGodIds = getFolloweeIdsForUser(userId, FollowingType.GOD);

        return godDao.getGodsPaginated(limit, lastGodId)
                .stream()
                .map(god -> {
                    GodForUser godForUser = new GodForUser();
                    godForUser.setGod(god);
                    godForUser.setFollowed(followedGodIds.contains(god.getGodId()));
                    return godForUser;
                }).toList();
    }

    public God getGod(String godId) {
        return godDao.getGod(godId).orElse(null);
    }


    public InfluencerInitResponse initInfluencer(InfluencerInitRequest influencerInitRequest) {
        String id = influencerDao.generateId();

        UploadInitReq uploadInitReq = influencerInitRequest.getUploadInitReq();

        Preconditions.checkState(uploadInitReq != null);
        String objectKeyFormat = getInfluencerImageUploadObjectKeyFormat(id);

        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() <= 10);

        UploadInitRes initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat,
                ContentUploadUtils.ContentTypeByExtension.IMAGE);
        if (!initRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new InfluencerInitResponse(null, initRes);
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

        Preconditions.checkState(urls.size() <= 10);

        influencerInitRequest.getInfluencer().setImageUrl(urls);

        influencerInitRequest.getInfluencer().setContentUploadStatus(ContentUploadStatus.PENDING);
        Influencer influencerResp = influencerDao.addInfluencer(influencerInitRequest.getInfluencer(), id);

        return new InfluencerInitResponse(influencerResp, initRes);
    }

    public InfluencerCompletionResponse postUploadUpdateInfluencer(InfluencerContentUploadReq influencerContentUploadReq) {
        Optional<Influencer> influencer = influencerDao.getInfleuncer(influencerContentUploadReq.getInfluencer().getUserId());
        Preconditions.checkState(influencer.isPresent());
        Preconditions.checkState(influencerContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() <= 10);

        List<String> urls =
                influencerContentUploadReq.getUploadCompletionReq()
                        .getUploadFileCompletionReqs()
                        .stream()
                        .map(uploadFileCompletionReq -> contentUploadUtils.getObjectUrl(uploadFileCompletionReq.getObjectKey()))
                        .sorted()
                        .toList();
        List<String> actualUrls = influencer.get().getImageUrl().stream().sorted().toList();
        Preconditions.checkState(actualUrls.equals(urls));

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(influencerContentUploadReq.getUploadCompletionReq(),
                        ContentUploadUtils.ContentTypeByExtension.IMAGE);

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new InfluencerCompletionResponse(influencerContentUploadReq.getInfluencer(),
                    uploadCompletionRes);
        }

        Influencer influencerCompleted = influencerDao.updateInfluencerStatus(influencerContentUploadReq.getInfluencer().getUserId(),
                ContentUploadStatus.PROCESSED);
        return new InfluencerCompletionResponse(influencerCompleted, uploadCompletionRes);
    }

    public List<Influencer> getInfluencers(int limit) {
        return influencerDao.getInfluencers(limit);
    }

    public List<Influencer> getNextPageOfInfluencers(int limit, String lastInfluencerId) {
        return influencerDao.getInfleuncersPaginated(limit, lastInfluencerId);
    }

    public List<InfluencerForUser> getInfluencersForUser(int limit, String userId) {
        Set<String> followedInfluencerIds = getFolloweeIdsForUser(userId, FollowingType.INFLUENCER);

        return influencerDao.getInfluencers(limit)
                .stream()
                .map(influencer -> {
                    InfluencerForUser influencerForUser = new InfluencerForUser();
                    influencerForUser.setInfluencer(influencer);
                    influencerForUser.setFollowed(followedInfluencerIds.contains(influencer.getUserId()));
                    influencerForUser.setNumOfPosts(postsDao.countPostsForInfluencer(userId));
                    return influencerForUser;
                }).toList();
    }

    public List<InfluencerForUser> getNextPageOfInfluencersForUser(int limit, String lastInfluencerId, String userId) {
        Set<String> followedInfluencerIds = getFolloweeIdsForUser(userId, FollowingType.INFLUENCER);

        return influencerDao.getInfleuncersPaginated(limit, lastInfluencerId)
                .stream()
                .map(influencer -> {
                    InfluencerForUser influencerForUser = new InfluencerForUser();
                    influencerForUser.setInfluencer(influencer);
                    influencerForUser.setFollowed(followedInfluencerIds.contains(influencer.getUserId()));
                    return influencerForUser;
                }).toList();
    }

    public Influencer getInfluencer(String userId) {
        return influencerDao.getInfleuncer(userId).orElse(null);
    }


    public PostInitResponse initPosts(String userAuthToken, PostInitRequest postInitReq) {
//        checkUserHasFirebaseAuth(postInitReq.getPost().getFromUserId());
        validateUserTokenIsValid(postInitReq.getPost().getFromUserId(), userAuthToken);
        validateUserPermissionForPost(
                postInitReq.getPost().getFromUserId(),
                postInitReq.getPost());

        if (postInitReq.getPost().getType() == null) {
            postInitReq.getPost().setType(PostType.TEXT);
        }
        String id = postsDao.generateId();
        UploadInitRes initRes = null;
        UploadInitReq uploadInitReq = postInitReq.getUploadInitReq();

        if (uploadInitReq != null) {
            if (postInitReq.getPost().getType() != PostType.TEXT) {
                ContentUploadUtils.ContentTypeByExtension contentTypeByExtension = null;
                String objectKeyFormat = null;
                switch (postInitReq.getPost().getType()) {
                    case VIDEO:
                        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() == 1);
                        objectKeyFormat = getPostVideoUploadObjectKeyFormat(id,
                                postInitReq.getPost().getFromUserId());
                        contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.VIDEO;
                        break;
                    case AUDIO:
                        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() == 1);
                        objectKeyFormat = getPostImageUploadObjectKeyFormat(id,
                                postInitReq.getPost().getFromUserId());
                        contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.AUDIO;
                        break;
                    case IMAGES:
                        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() <= 10);
                        objectKeyFormat = getPostAudioUploadObjectKeyFormat(id,
                                postInitReq.getPost().getFromUserId());
                        contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.IMAGE;
                        break;
                    case TEXT:
                        break;
                }

                initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat, contentTypeByExtension);
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


    public PostCompletionResponse postUploadUpdatePost(String userAuthToken, PostContentUploadReq postContentUploadReq) {
//        checkUserHasFirebaseAuth(postContentUploadReq.getPost().getFromUserId());
        validateUserTokenIsValid(postContentUploadReq.getPost().getFromUserId(), userAuthToken);
        validateUserPermissionForPost(
                postContentUploadReq.getPost().getFromUserId(),
                postContentUploadReq.getPost());

        Optional<Post> post = postsDao.getPost(postContentUploadReq.getPost().getPostId());
        Preconditions.checkState(post.isPresent());
        Preconditions.checkState(post.get().getType().equals(postContentUploadReq.getPost().getType()));

        String objectKey = null;
        String url = null;
        ContentUploadStatus contentUploadStatus = null;
        ContentUploadUtils.ContentTypeByExtension contentTypeByExtension = null;
        switch (post.get().getType()) {
            case VIDEO:
                Preconditions.checkState(postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() == 1);
                objectKey = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().get(0).getObjectKey();
                url = contentUploadUtils.getObjectUrl(objectKey);
                Preconditions.checkState(post.get().getVideoUrl().equals(url));
                contentUploadStatus = ContentUploadStatus.RAW_UPLOADED;
                contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.VIDEO;
                break;
            case AUDIO:
                Preconditions.checkState(postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() == 1);
                objectKey = postContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().get(0).getObjectKey();
                url = contentUploadUtils.getObjectUrl(objectKey);
                Preconditions.checkState(post.get().getAudioUrl().equals(url));
                contentUploadStatus = ContentUploadStatus.RAW_UPLOADED;
                contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.AUDIO;
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

                List<String> actualUrls = post.get().getImagesUrl().stream().sorted().toList();
                Preconditions.checkState(actualUrls.equals(urls));
                contentUploadStatus = ContentUploadStatus.PROCESSED;
                contentTypeByExtension = ContentUploadUtils.ContentTypeByExtension.IMAGE;
                break;
            case TEXT:
                contentUploadStatus = ContentUploadStatus.SUCCESS_NO_CONTENT;
                break;
        }

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(postContentUploadReq.getUploadCompletionReq(),
                        contentTypeByExtension);

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new PostCompletionResponse(postContentUploadReq.getPost(), uploadCompletionRes);
        }

        Post completePost = postsDao.updatePostStatus(postContentUploadReq.getPost().getPostId(),
                contentUploadStatus);
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
        return postsDao.getPostsPaginated(id, associationType, limit, Optional.ofNullable(lastPostId),
                true);
    }

    public List<Post> getPostsOfGod(int limit, String godId) {
        Optional<God> god = godDao.getGod(godId);
        Preconditions.checkState(god.isPresent());
        return postsDao.getPostOfGod(godId, god.get().getGodName(), limit, Optional.empty(), true);
    }

    public FeedPageResponse getFeedOfMandir(int limit, String mandirId, String forUserId,
                                            boolean onlyProcessed,
                                            Optional<String> lastPostId) {
        Optional<Mandir> mandir = mandirDao.getMandir(mandirId);
        Preconditions.checkState(mandir.isPresent());
        boolean isUserAuthorizedForPosts =
                userAuthDao.isUserAuthorized(forUserId, mandirId, AuthAssociationType.MANDIR);
        List<FeedItem> feedItems =
                postsDao.getPostsPaginated(
                                mandirId,
                                PostAssociationType.MANDIR,
                                limit, lastPostId,
                                !isUserAuthorizedForPosts)
                        .stream()
                        .map(this::postToFeedItem)
                        .toList();

        FeedItemHeader feedItemHeader = new FeedItemHeader();

        MandirFeedHeader mandirFeedHeader = new MandirFeedHeader();
        mandirFeedHeader.setName(mandir.get().getName());
        mandirFeedHeader.setDescription(mandir.get().getDescription());
        mandirFeedHeader.setImageUrls(mandir.get().getImageUrl());
        mandirFeedHeader.setMyProfilePage(isUserAuthorizedForPosts);
        mandirFeedHeader.setCurrentUserFollowing(followingsDao.isUserFollowing(forUserId, mandirId, FollowingType.MANDIR));

        int followingCount = followingsDao.countFollowingsForFollowee(mandirId, FollowingType.MANDIR);
        mandirFeedHeader.setFollowersCount(followingCount);

        if (isUserAuthorizedForPosts) {
            List<PostAMessageInfo.Tag> tags =
                    godDao.getGods(1000)
                            .stream().map(g -> {
                                PostAMessageInfo.Tag tag = new PostAMessageInfo.Tag();
                                tag.setId(g.getGodId());
                                tag.setName(g.getGodName());
                                return tag;
                            }).toList();
            PostAMessageInfo postAMessageInfo = new PostAMessageInfo();
            postAMessageInfo.setTags(tags);
            mandirFeedHeader.setPostAMessageInfo(postAMessageInfo);
        }

        feedItemHeader.setMandirFeedHeader(mandirFeedHeader);

        FeedPageResponse feedPageResponse = new FeedPageResponse();
        feedPageResponse.setFeedItemHeader(feedItemHeader);
        feedPageResponse.setFeedItems(feedItems);

        MandirFeedPaginationKey feedePaginationKey = new MandirFeedPaginationKey();
        if (!feedItems.isEmpty()) {
            feedePaginationKey.setLastPostId(feedItems.get(feedItems.size() - 1).getPost().getPostId());
        }
        CuratedFeedPaginationKey curatedFeedPaginationKey = new CuratedFeedPaginationKey();
        curatedFeedPaginationKey.setMandirFeedPaginationKey(feedePaginationKey);
        feedPageResponse.setCuratedFeedPaginationKey(curatedFeedPaginationKey);

        return feedPageResponse;
    }

    public FeedPageResponse getFeedOfGod(int limit, String godId, String forUserId,
                                         boolean onlyProcessed,
                                         Optional<String> lastPostId) {
        Optional<God> god = godDao.getGod(godId);
        Preconditions.checkState(god.isPresent());
        boolean isUserAuthorizedForPosts = userAuthDao.isUserAuthorized(forUserId, godId, AuthAssociationType.GOD);
        List<FeedItem> feedItems =
                postsDao.getPostOfGod(godId, god.get().getGodName(),
                                limit, lastPostId,
                                !isUserAuthorizedForPosts)
                        .stream()
                        .map(this::postToFeedItem)
                        .toList();

        FeedItemHeader feedItemHeader = new FeedItemHeader();

        GodFeedHeader godFeedHeader = new GodFeedHeader();
        godFeedHeader.setName(god.get().getGodName());
        godFeedHeader.setDescription(god.get().getDescription());
        godFeedHeader.setImageUrls(god.get().getImageUrl());
        godFeedHeader.setMyProfilePage(isUserAuthorizedForPosts);
        godFeedHeader.setCurrentUserFollowing(followingsDao.isUserFollowing(forUserId, godId, FollowingType.GOD));

        int followingCount = followingsDao.countFollowingsForFollowee(godId, FollowingType.GOD);
        godFeedHeader.setFollowersCount(followingCount);

        if (isUserAuthorizedForPosts) {
            List<PostAMessageInfo.Tag> tags =
                    godDao.getGods(1000)
                            .stream().map(g -> {
                                PostAMessageInfo.Tag tag = new PostAMessageInfo.Tag();
                                tag.setId(g.getGodId());
                                tag.setName(g.getGodName());
                                return tag;
                            }).toList();
            PostAMessageInfo postAMessageInfo = new PostAMessageInfo();
            postAMessageInfo.setTags(tags);
            godFeedHeader.setPostAMessageInfo(postAMessageInfo);
        }

        feedItemHeader.setGodFeedHeader(godFeedHeader);

        FeedPageResponse feedPageResponse = new FeedPageResponse();
        feedPageResponse.setFeedItemHeader(feedItemHeader);
        feedPageResponse.setFeedItems(feedItems);

        GodFeedePaginationKey godFeedePaginationKey = new GodFeedePaginationKey();
        if (!feedItems.isEmpty()) {
            godFeedePaginationKey.setLastPostId(feedItems.get(feedItems.size() - 1).getPost().getPostId());
        }
        CuratedFeedPaginationKey curatedFeedPaginationKey = new CuratedFeedPaginationKey();
        curatedFeedPaginationKey.setGodFeedePaginationKey(godFeedePaginationKey);
        feedPageResponse.setCuratedFeedPaginationKey(curatedFeedPaginationKey);

        return feedPageResponse;
    }

    public List<Post> getPostsOfGodPaginated(int limit, String godId,
                                             String lastPostId) {
        Optional<God> god = godDao.getGod(godId);
        Preconditions.checkState(god.isPresent());
        return postsDao.getPostOfGod(godId, god.get().getGodName(), limit, Optional.ofNullable(lastPostId), true);
    }

    public List<Post> getPostsOfInfluencer(int limit, String id, boolean onlyProcessed) {
        return postsDao.getPostForInfluencer(id, limit, Optional.empty(), onlyProcessed);
    }

    public FeedPageResponse getFeedOfInfluencer(int limit, String influencerId, String forUserId,
                                                boolean onlyProcessed,
                                                Optional<String> lastPostId) {
        Optional<Influencer> influencer = influencerDao.getInfleuncer(influencerId);
        Preconditions.checkState(influencer.isPresent());
        boolean isUserAuthorizedForPosts = influencerId.equals(forUserId) ||
                userAuthDao.isUserAuthorized(forUserId, influencerId, AuthAssociationType.INFLUENCER);
        List<FeedItem> feedItems =
                postsDao.getPostForInfluencer(
                                influencerId,
                                limit, lastPostId,
                                !isUserAuthorizedForPosts)
                        .stream()
                        .map(this::postToFeedItem)
                        .toList();

        FeedItemHeader feedItemHeader = new FeedItemHeader();

        InfluencerFeedHeader influencerFeedHeader = new InfluencerFeedHeader();
        influencerFeedHeader.setName(influencer.get().getName());
        influencerFeedHeader.setDescription(influencer.get().getDescription());
        influencerFeedHeader.setImageUrls(influencer.get().getImageUrl());
        influencerFeedHeader.setMyProfilePage(isUserAuthorizedForPosts);
        influencerFeedHeader.setCurrentUserFollowing(
                followingsDao.isUserFollowing(forUserId,
                influencerId, FollowingType.INFLUENCER));


        int followingCount = followingsDao.countFollowingsForFollowee(influencerId, FollowingType.INFLUENCER);
        influencerFeedHeader.setFollowersCount(followingCount);

        if (isUserAuthorizedForPosts) {
            List<PostAMessageInfo.Tag> tags =
                    godDao.getGods(1000)
                            .stream().map(g -> {
                                PostAMessageInfo.Tag tag = new PostAMessageInfo.Tag();
                                tag.setId(g.getGodId());
                                tag.setName(g.getGodName());
                                return tag;
                            }).toList();
            PostAMessageInfo postAMessageInfo = new PostAMessageInfo();
            postAMessageInfo.setTags(tags);
            influencerFeedHeader.setPostAMessageInfo(postAMessageInfo);
        }

        feedItemHeader.setInfluencerFeedHeader(influencerFeedHeader);

        FeedPageResponse feedPageResponse = new FeedPageResponse();
        feedPageResponse.setFeedItemHeader(feedItemHeader);
        feedPageResponse.setFeedItems(feedItems);

        InfluencerFeedPaginationKey feedePaginationKey = new InfluencerFeedPaginationKey();
        if (!feedItems.isEmpty()) {
            feedePaginationKey.setLastPostId(feedItems.get(feedItems.size() - 1).getPost().getPostId());
        }
        CuratedFeedPaginationKey curatedFeedPaginationKey = new CuratedFeedPaginationKey();
        curatedFeedPaginationKey.setInfluencerFeedPaginationKey(feedePaginationKey);
        feedPageResponse.setCuratedFeedPaginationKey(curatedFeedPaginationKey);

        return feedPageResponse;
    }

    public List<Post> getPostsByInfluencerPaginated(int limit, String id,
                                                    String lastPostId, boolean onlyProcessed) {
        return postsDao.getPostForInfluencer(id, limit, Optional.ofNullable(lastPostId), onlyProcessed);
    }

    public DarshanInitResponse initDarshan(DarshanInitRequest darshanInitRequest) {
        darshanInitRequest.getDarshan().setVideoUploadStatus(ContentUploadStatus.PENDING);
        String id = darshanDao.generateId();

        UploadInitReq uploadInitReq = darshanInitRequest.getUploadInitReq();

        Preconditions.checkState(uploadInitReq != null);
        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() == 1);
        String objectKeyFormat = getDarshanVideoUploadObjectKeyFormat(id);

        UploadInitRes initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat,
                ContentUploadUtils.ContentTypeByExtension.VIDEO);
        if (!initRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new DarshanInitResponse(null, initRes);
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
        Preconditions.checkState(urls.size() == 1);
        darshanInitRequest.getDarshan().setVideoUrl(urls.get(0));

        Darshan darshanRes = darshanDao.initDarshan(darshanInitRequest.getDarshan(), id);
        return new DarshanInitResponse(darshanRes, initRes);
    }

    public DarshanCompletionResponse postUploadUpdateDarshan(DarshanContentUploadReq darshanContentUploadReq) {
        Optional<Darshan> darshan = darshanDao.getDarshan(darshanContentUploadReq.getDarshan().getDarshanId());
        Preconditions.checkState(darshan.isPresent());
        Preconditions.checkState(darshanContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() == 1);

        String objectKey = darshanContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().get(0).getObjectKey();
        String url = contentUploadUtils.getObjectUrl(objectKey);
        Preconditions.checkState(darshan.get().getVideoUrl().equals(url));

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(darshanContentUploadReq.getUploadCompletionReq(),
                        ContentUploadUtils.ContentTypeByExtension.VIDEO);

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new DarshanCompletionResponse(darshanContentUploadReq.getDarshan(),
                    uploadCompletionRes);
        }
        Darshan darshanCompleted = darshanDao.updateDarshanStatus(darshan.get().getDarshanId(),
                ContentUploadStatus.RAW_UPLOADED);
        return new DarshanCompletionResponse(darshanCompleted, uploadCompletionRes);
    }

    public List<Darshan> getCuratedDarshans() {
        Optional<CuratedDarshan> curatedDarshan =
                curatedDarshanDao.getLastNCuratedDarshan(1).stream().findFirst();

        Map<String, List<String>> curatedDarshans = curatedDarshan.get().getGodToDarshanIds();
        List<Darshan> darshans = new ArrayList<>();

        Map<String, Integer> godNameToCounter = new TreeMap<>(curatedDarshans.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));

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

    public MandirInitResponse initMandir(MandirInitRequest mandirInitRequest) {
        String id = influencerDao.generateId();

        UploadInitReq uploadInitReq = mandirInitRequest.getUploadInitReq();

        Preconditions.checkState(uploadInitReq != null);
        String objectKeyFormat = getMandirImageUploadObjectKeyFormat(id);

        Preconditions.checkState(uploadInitReq.getUploadFileInitReqs().size() <= 10);
        // TODO: add content type
        UploadInitRes initRes = contentUploadUtils.initUpload(uploadInitReq, objectKeyFormat,
                ContentUploadUtils.ContentTypeByExtension.IMAGE);
        if (!initRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new MandirInitResponse(null, initRes);
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

        Preconditions.checkState(urls.size() <= 10);

        mandirInitRequest.getMandir().setImageUrl(urls);

        mandirInitRequest.getMandir().setContentUploadStatus(ContentUploadStatus.PENDING);

        Mandir mandirRes = mandirDao.initMandir(mandirInitRequest.getMandir(), id);
        return new MandirInitResponse(mandirRes, initRes);
    }


    public MandirCompletionResponse postUploadUpdateMandir(MandirContentUploadReq mandirContentUploadReq) {
        Optional<Mandir> mandir = mandirDao.getMandir(mandirContentUploadReq.getMandir().getMandirId());
        Preconditions.checkState(mandir.isPresent());
        Preconditions.checkState(mandirContentUploadReq.getUploadCompletionReq().getUploadFileCompletionReqs().size() <= 10);

        List<String> urls =
                mandirContentUploadReq.getUploadCompletionReq()
                        .getUploadFileCompletionReqs()
                        .stream()
                        .map(uploadFileCompletionReq -> contentUploadUtils.getObjectUrl(uploadFileCompletionReq.getObjectKey()))
                        .sorted()
                        .toList();
        List<String> actualUrls = mandir.get().getImageUrl().stream().sorted().toList();
        Preconditions.checkState(actualUrls.equals(urls));

        UploadCompletionRes uploadCompletionRes =
                contentUploadUtils.completeUpload(mandirContentUploadReq.getUploadCompletionReq(),
                        ContentUploadUtils.ContentTypeByExtension.IMAGE);

        if (!uploadCompletionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            return new MandirCompletionResponse(mandirContentUploadReq.getMandir(),
                    uploadCompletionRes);
        }

        Mandir mandirCompleted = mandirDao.updateMandirStatus(mandirContentUploadReq.getMandir().getMandirId(),
                ContentUploadStatus.PROCESSED);
        return new MandirCompletionResponse(mandirCompleted, uploadCompletionRes);
    }

    public Mandir getMandir(String mandirId) {
        return mandirDao.getMandir(mandirId).orElse(null);
    }

    public List<Mandir> getMandirs(int limit) {
        return mandirDao.getMandirs(limit);
    }

    public List<MandirForUser> getMandirsForUser(int limit, String userId) {
        Set<String> followedMandirIds = getFolloweeIdsForUser(userId, FollowingType.MANDIR);
        return mandirDao.getMandirs(limit)
                .stream()
                .map(mandir -> {
                    MandirForUser m = new MandirForUser();
                    m.setMandir(mandir);
                    m.setFollowed(followedMandirIds.contains(mandir.getMandirId()));
                    return m;
                }).toList();
    }

    public List<Mandir> getMandirsPaginated(int limit, String lastMandirId) {
        return mandirDao.getMandirsPaginated(limit, lastMandirId);
    }

    public List<MandirForUser> getMandirsForUser(int limit, String lastMandirId, String userId) {
        Set<String> followedMandirIds = getFolloweeIdsForUser(userId, FollowingType.MANDIR);
        return mandirDao.getMandirsPaginated(limit, lastMandirId)
                .stream()
                .map(mandir -> {
                    MandirForUser m = new MandirForUser();
                    m.setMandir(mandir);
                    m.setFollowed(followedMandirIds.contains(mandir.getMandirId()));
                    return m;
                }).toList();
    }

    public List<Page> getAuthorizedPagesForUser(String userId) {
        List<Page> pages =
                userAuthDao.getAuthWithAdminPermission(userId)
                        .stream()
                        .map(authForUser -> {
                            Page page = new Page();
                            switch (authForUser.getAssociationType()) {
                                case GOD:
                                    God god = godDao.getGod(authForUser.getResourceId()).get();
                                    page.setTitle(god.getGodName());
                                    page.setImageUrl(god.getImageUrl().get(0));
                                    page.setPageType(PageType.GOD);

                                    MyGodPageInfo myGodPageInfo = new MyGodPageInfo();
                                    myGodPageInfo.setGodId(god.getGodId());

                                    page.setMyGodPageInfo(myGodPageInfo);
                                    break;
                                case MANDIR:
                                    Mandir mandir = mandirDao.getMandir(authForUser.getResourceId()).get();
                                    page.setTitle(mandir.getName());
                                    page.setImageUrl(mandir.getImageUrl().get(0));
                                    page.setPageType(PageType.MANDIR);

                                    MyMandirPageInfo myMandirPageInfo = new MyMandirPageInfo();
                                    myMandirPageInfo.setMandirId(mandir.getMandirId());

                                    page.setMyMandirPageInfo(myMandirPageInfo);
                                    break;
                                case INFLUENCER:
                                    Influencer influencer = influencerDao.getInfleuncer(authForUser.getResourceId()).get();
                                    page.setTitle(influencer.getName());
                                    page.setImageUrl(influencer.getImageUrl().get(0));
                                    page.setPageType(PageType.INFLUENCER);

                                    MyInfluencerPageInfo myInfluencerPageInfo =
                                            new MyInfluencerPageInfo();
                                    myInfluencerPageInfo.setInfluencerId(influencer.getUserId());

                                    page.setMyInfluencerPageInfo(myInfluencerPageInfo);
                                    break;
                                default:
                                    throw new IllegalStateException("unhandled association type:" + authForUser.getAssociationType());
                            }
                            return page;
                        }).sorted(Comparator.comparing(Page::getPageType))
                        .toList();
        return pages;
    }

    public void addMandirAuth(String mandirId, String userId) {
        validateUserHasFirebaseAuth(userId);
        userAuthDao.addUserAuth(mandirId, userId, AuthType.ADMIN, AuthAssociationType.MANDIR);
    }

    public void addInfluencerAuth(String influencerId, String userId) {
        validateUserHasFirebaseAuth(userId);
        userAuthDao.addUserAuth(influencerId, userId, AuthType.ADMIN, AuthAssociationType.INFLUENCER);
    }

    public void addGodAuth(String godId, String userId) {
        validateUserHasFirebaseAuth(userId);
        userAuthDao.addUserAuth(godId, userId, AuthType.ADMIN, AuthAssociationType.GOD);
    }

    public List<Mandir> getAuthorizedMandirForUser(String userId) {
        validateUserHasFirebaseAuth(userId);
        return userAuthDao.getAuthWithAdminPermission(userId)
                .stream()
                .filter(auth -> auth.getAssociationType().equals(AuthAssociationType.MANDIR))
                .map(auth -> mandirDao.getMandir(auth.getResourceId()).get())
                .toList();
    }

    public FeedPageResponse getCuratedFeedPage(CuratedFeedRequest curatedFeedRequest) {
        CuratedFeedResponse curatedFeedResponse = getCuratedFeed(curatedFeedRequest);
        List<FeedItem> feedItems =
                curatedFeedResponse.getPosts()
                        .stream()
                        .map(this::postToFeedItem)
                        .toList();

        int followingsCount =
                followingsDao.getFollowingsForUser(curatedFeedRequest.getUserId()).size();

        FeedPageResponse feedPageResponse = getFeedPageResponse(followingsCount, feedItems, curatedFeedResponse);

        return feedPageResponse;
    }

    private FeedItem postToFeedItem(Post post) {
        FeedItem feedItem = new FeedItem();
        feedItem.setPost(post);
        switch (post.getAssociationType()) {
            case MANDIR:
                Optional<Mandir> mandir = mandirDao.getMandir(post.getAssociatedMandirId());
                Preconditions.checkState(mandir.isPresent());
                mandir.ifPresent(m -> {
                    feedItem.setFrom(m.getName());
                    feedItem.setFromImgUrl(m.getImageUrl());
                });
                break;
            case INFLUENCER:
                Optional<Influencer> influencer = influencerDao.getInfleuncer(post.getAssociatedInfluencerId());
                Preconditions.checkState(influencer.isPresent());
                influencer.ifPresent(i -> {
                    feedItem.setFrom(i.getName());
                    feedItem.setFromImgUrl(i.getImageUrl());
                });
                break;
            case GOD:
                Optional<God> god = godDao.getGod(post.getAssociatedGodId());
                Preconditions.checkState(god.isPresent());
                god.ifPresent(g -> {
                    feedItem.setFrom(g.getGodName());
                    feedItem.setFromImgUrl(g.getImageUrl());
                });
                break;
        }
        return feedItem;
    }

    private static FeedPageResponse getFeedPageResponse(int followingsCount, List<FeedItem> feedItems, CuratedFeedResponse curatedFeedResponse) {
        FeedItemHeader feedItemHeader = new FeedItemHeader();
        MyFeedHeader myFeedHeader = new MyFeedHeader();
        myFeedHeader.setFollowingsCount(followingsCount);
        feedItemHeader.setMyFeedHeader(myFeedHeader);

        FeedPageResponse feedPageResponse = new FeedPageResponse();
        feedPageResponse.setFeedItems(feedItems);
        feedPageResponse.setFeedItemHeader(feedItemHeader);
        feedPageResponse.setCuratedFeedPaginationKey(curatedFeedResponse.getCuratedFeedPaginationKey());
        return feedPageResponse;
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
        log.info("scored content videos:{}", scoredContentVideosPostIds);

        List<String> scoredContentAudioPostIds =
                scoredCollectionName.map(s -> scoredContentDao.getScoredContentSorted(s, 1000, PostType.AUDIO)
                        .stream().map(ScoredContent::getPostId).toList()).orElse(Collections.emptyList());

        List<String> newVideosPostIds = newPostsDao.getNewPostsByTypeNext(PostType.VIDEO, 100, Optional.empty())
                .stream().map(NewPost::getPostId).toList();

        List<String> newAudioPostIds = newPostsDao.getNewPostsByTypeNext(PostType.AUDIO, 100, Optional.empty())
                .stream().map(NewPost::getPostId).toList();

        Set<String> viewedPostIdsForUser = new HashSet<>(viewsDao.getViewsForUser(userId, 10000)
                .stream().map(View::getPostId).toList());
        log.info("viewedPostIdsForUser:{}", viewedPostIdsForUser);

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
                        newVideosPostIdsCount.get() > 0 ||
                        scoredContentAudioPostIdsCount.get() > 0 ||
                        newAudioPostIdsCount.get() > 0)
        ) {

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

    private Set<String> getFolloweeIdsForUser(String userId, FollowingType followingType) {
        return followingsDao.getFollowingsForUser(userId, followingType)
                .stream()
                .map(Following::getFolloweeId)
                .collect(Collectors.toSet());
    }

    private void checkUserExists(String userId) {
        Preconditions.checkState(userRegistrationDao.getUser(userId).isPresent());
    }

    private void validateUserHasFirebaseAuth(String userId) {
        Optional<User> user = userRegistrationDao.getUser(userId);
        if (user.isEmpty() || StringUtils.isEmpty(user.get().getAuthUserId())) {
            String msg = "user is not authorized:" + userId;
            log.error(msg);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
        }
    }

    private void validateUserTokenIsValid(String userId, String userAuthToken) {
        Optional<FirebaseClient.FirebaseUserInfo> userInfo =
                firebaseClient.getUserFromToken(userAuthToken);

        Optional<User> user = userRegistrationDao.getUser(userId);

        if (userInfo.isEmpty() ||
                user.isEmpty() ||
                StringUtils.isEmpty(user.get().getAuthUserId()) ||
                !userInfo.get().getUid().equals(user.get().getAuthUserId())) {
            String msg = String.format("user is not authorized userId:%s, token:%s", userId, userAuthToken);
            log.error(msg);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
        }
    }

    private void validateUserPermissionForPost(String userId, Post post) {
        if (!checkUserHasPermissionForResource(userId, post)) {
            String msg = String.format("user is not authorized userId:%s, post:%s", userId, post);
            log.error(msg);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
        }
    }

    private boolean checkUserHasPermissionForResource(String userId, Post post) {
        switch (post.getAssociationType()) {
            case GOD:
                String godId = post.getAssociatedGodId();
                return userAuthDao.isUserAuthorized(userId, godId, AuthAssociationType.GOD);
            case MANDIR:
                String mandirId = post.getAssociatedMandirId();
                return userAuthDao.isUserAuthorized(userId, mandirId, AuthAssociationType.MANDIR);
            case INFLUENCER:
                String influencerId = post.getAssociatedInfluencerId();
                return userAuthDao.isUserAuthorized(userId, influencerId, AuthAssociationType.INFLUENCER);
        }

        return false;
    }
}
