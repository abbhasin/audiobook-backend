package com.enigma.audiobook.backend.service;

import com.enigma.audiobook.backend.dao.GodDao;
import com.enigma.audiobook.backend.dao.InfluencerDao;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.dao.UserRegistrationDao;
import com.enigma.audiobook.backend.models.*;
import com.enigma.audiobook.backend.models.requests.GodImageUploadReq;
import com.enigma.audiobook.backend.models.requests.InfluencerImageUploadReq;
import com.enigma.audiobook.backend.models.requests.PostContentUploadReq;
import com.enigma.audiobook.backend.models.requests.UserRegistrationInfo;
import com.enigma.audiobook.backend.models.responses.GodInitResponse;
import com.enigma.audiobook.backend.models.responses.InfluencerInitResponse;
import com.enigma.audiobook.backend.models.responses.PostInitResponse;
import com.enigma.audiobook.backend.models.responses.UserAssociationResponse;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.*;

@Service
@Data
public class OneGodService {

    private final UserRegistrationDao userRegistrationDao;
    private final GodDao godDao;
    private final InfluencerDao influencerDao;
    private final PostsDao postsDao;

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
}
