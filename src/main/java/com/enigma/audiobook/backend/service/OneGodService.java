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


    public PostCompletionResponse postUploadUpdatePost(PostContentUploadReq postContentUploadReq) {
        checkAuthorization(postContentUploadReq.getPost().getFromUserId());
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
        return postsDao.getPostsPaginated(id, associationType, limit, lastPostId);
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

    public FeedPageResponse getCuratedFeedPage(CuratedFeedRequest curatedFeedRequest) {
        CuratedFeedResponse curatedFeedResponse = getCuratedFeed(curatedFeedRequest);
        List<FeedItem> feedItems =
                curatedFeedResponse.getPosts()
                        .stream()
                        .map(post -> {
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
                        }).toList();

        int followingsCount =
                followingsDao.getFollowingsForUser(curatedFeedRequest.getUserId()).size();

        FeedItemHeader feedItemHeader = new FeedItemHeader();
        feedItemHeader.setFollowingsCount(followingsCount);

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
