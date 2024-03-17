package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.models.PostType;
import com.enigma.audiobook.backend.utils.SerDe;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Component
public class ContentEncodingScheduler implements Runnable {
    private static final int jobRunnableSize = 5;
    private static final ExecutorService jobRunnableExecutors = Executors.newFixedThreadPool(jobRunnableSize);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final PostsDao postsDao;
    private final DarshanDao darshanDao;
    private final JobQueue jobQueue;
    private final ContentEncodingHandler contentEncodingHandler;
    private final String db;

    public ContentEncodingScheduler(PostsDao postsDao, DarshanDao darshanDao,
                                    ContentEncodingHandler contentEncodingHandler,
                                    @Value("${mongo.database}") String db) {
        this.postsDao = postsDao;
        this.darshanDao = darshanDao;
        this.contentEncodingHandler = contentEncodingHandler;
        this.db = db;
        this.jobQueue = new JobQueue();

        for (int i = 0; i < jobRunnableSize; i++) {
            jobRunnableExecutors.submit(
                    new JobRunnable(jobQueue, contentEncodingHandler, db, postsDao, darshanDao));
        }

        scheduler.scheduleWithFixedDelay(this, 1, 10, TimeUnit.MINUTES);
    }


    public void run() {
        try {
            List<CEJob> postVideoJobs =
                    postsDao.getPostsByTypeAndStatus(PostType.VIDEO, 20, Optional.empty(), ContentUploadStatus.RAW_UPLOADED)
                            .stream()
                            .map(post -> new CEJob(post.getPostId(), JobType.POST))
                            .toList();
            postVideoJobs.forEach(jobQueue::add);

            List<CEJob> postAudioJobs =
                    postsDao.getPostsByTypeAndStatus(PostType.VIDEO, 20, Optional.empty(), ContentUploadStatus.RAW_UPLOADED)
                            .stream()
                            .map(post -> new CEJob(post.getPostId(), JobType.POST))
                            .toList();
            postAudioJobs.forEach(jobQueue::add);

            List<CEJob> darshanJobs =
                    darshanDao.getDarshanByStatus(ContentUploadStatus.RAW_UPLOADED, 20)
                            .stream()
                            .map(darshan -> new CEJob(darshan.getDarshanId(), JobType.DARSHAN))
                            .toList();
            darshanJobs.forEach(jobQueue::add);
        } catch (Throwable t) {
            log.error("unable to schedule content encoding", t);
        }

    }

    public static class JobRunnable implements Runnable {
        private static final SerDe serDe = new SerDe();
        private final JobQueue jobQueue;
        private final ContentEncodingHandler contentEncodingHandler;
        private final String db;
        private final PostsDao postsDao;
        private final DarshanDao darshanDao;

        public JobRunnable(JobQueue jobQueue, ContentEncodingHandler contentEncodingHandler, String db, PostsDao postsDao, DarshanDao darshanDao) {
            this.jobQueue = jobQueue;
            this.contentEncodingHandler = contentEncodingHandler;
            this.db = db;
            this.postsDao = postsDao;
            this.darshanDao = darshanDao;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    CEJob ceJob = jobQueue.take();
                    switch (ceJob.getJobType()) {
                        case POST:
                            Optional<Post> post = postsDao.getPost(ceJob.getId());
                            Preconditions.checkState(post.isPresent());
                            contentEncodingHandler.encodeContentForCollectionEntry(
                                    db,
                                    ceJob.getJobType().getCollection(),
                                    serDe.toJson(post.get()));
                            break;
                        case DARSHAN:
                            Optional<Darshan> darshan = darshanDao.getDarshan(ceJob.getId());
                            Preconditions.checkState(darshan.isPresent());
                            contentEncodingHandler.encodeContentForCollectionEntry(
                                    db,
                                    ceJob.getJobType().getCollection(),
                                    serDe.toJson(darshan.get()));
                            break;
                    }
                } catch (Throwable t) {
                    log.error("unable to process CEJob", t);
                }

                sleep();
            }
        }

        private void sleep() {
            try {
                Thread.sleep(1000l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class JobQueue {
        private final LinkedBlockingQueue<CEJob> linkedBlockingQueue = new LinkedBlockingQueue<>(60);
        private final Set<CEJob> queuedJobsSet = new HashSet<>();

        @SneakyThrows
        public void add(CEJob ceJob) {
            if (queuedJobsSet.contains(ceJob)) {
                log.info("CEJob already exists:{}", ceJob);
                return;
            }

            linkedBlockingQueue.put(ceJob);
            queuedJobsSet.add(ceJob);
        }

        @SneakyThrows
        public CEJob take() {
            CEJob ceJob = linkedBlockingQueue.take();
            queuedJobsSet.remove(ceJob);
            return ceJob;
        }
    }

    @Data
    public static class CEJob {
        private final String id;
        private final JobType jobType;
    }

    public enum JobType {
        POST(PostsDao.POSTS_COLLECTION),
        DARSHAN(DarshanDao.DARSHAN_REG_COLLECTION);

        private String collection;

        JobType(String col) {
            this.collection = col;
        }

        public String getCollection() {
            return collection;
        }
    }
}
