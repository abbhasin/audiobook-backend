package com.enigma.audiobook.backend.jobs;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ContentEncoderV2 {
    static final float THUMBNAIL_RATIO_OF_TOTAL_CONTENT = 0.2f;

    public static void main(String[] args) throws Exception {

        String inputFile = "/Users/akhil/Downloads/videos/shiv_ji_video_bhajan.mp4";
        String outputFile = "/Users/akhil/Downloads/videos/shiv_ji_bhajan_hls";
        updateVideoContentToDir(inputFile, outputFile);

//        System.out.println(System.getProperty("java.io.tmpdir"));
//        String outputThumbnailFile = "/Users/akhil/Downloads/test/video2/ID-34kjbnwed34/hls/thumbnail.jpg";
//        String outFile2 = "/Users/akhil/Downloads/test/video2/raw/videoplayback.mp4";
//        String outTNFile = "/Users/akhil/Downloads/test/video2/raw/thumbnail.jpg";
//        String outputThumbnailDir = "/Users/akhil/Downloads/test/video2/ID-34kjbnwed34/hls";
//        getContentDurationSec(outFile2);
////        generateThumbnail(outFile2, outputThumbnailFile);
//        generateThumbnailToDir(outFile2, outputThumbnailDir);

//
//        String inputImg = "/Users/akhil/Downloads/IMG_0728.PNG";
//        String outputImg = "/Users/akhil/Downloads/IMG_0728.JPG";
//        updateImageContent(inputImg, outputImg);

//        String inputAudio = "/Users/akhil/Downloads/test/audio/raw/town-10169.mp3";
//        String outputAudio = "/Users/akhil/Downloads/test/audio/ID-12345jlnfwe245/hls/town-10169.m3u8";
//        updateAudioContent(inputAudio, outputAudio);

    }

    public static void updateImageContentToDir(String inputFile, String outputFileDir) throws IOException, InterruptedException {
        updateImageContent(inputFile, getFilePath(inputFile, outputFileDir, "jpg"));
    }

    public static void updateImageContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        // scale=-2:480 maintains the same aspect ration as original video
        String[] cmd = new String[]{"-i", inputFile, "-vf", "scale=-2:480", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    public static void updateAudioContentToDir(String inputFile, String outputFileDir) throws IOException, InterruptedException {
        updateAudioContent(inputFile, getHLSFilePath(inputFile, outputFileDir));
    }

    public static void updateAudioContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        String[] cmd = new String[]{"-i", inputFile, "-c:a", "aac", "-b:a", "128k",
                "-force_key_frames", "expr:gte(t,n_forced*10)",
                "-hls_time", "10",
                "-hls_list_size", "0", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    public static void updateVideoContentToDir(String inputFile, String outputFileDir) throws IOException, InterruptedException {
        updateVideoContent(inputFile, getHLSFilePath(inputFile, outputFileDir));
    }

    public static void updateVideoContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        // scale=-2:480 maintains the same aspect ration as original video
        String[] cmd = new String[]{"-i", inputFile, "-vf", "scale=-2:480", "-c:v", "h264", "-b:v", "500k",
                "-c:a", "aac", "-b:a", "128k", "-force_key_frames", "expr:gte(t,n_forced*10)", "-hls_time", "10", "-hls_list_size", "0", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    public static void generateThumbnailToDir(String masterFilePath, String outputThumbnailDir) throws Exception {
        File file = new File(masterFilePath);
        String outputFileNamePrefix = file.getName().substring(0, file.getName().lastIndexOf("."));
        String outputFileName = String.format("%s_%s.%s", outputFileNamePrefix, "thumbnail", "jpg");
        String outputFilePath = String.format("%s/%s", outputThumbnailDir, outputFileName);

        generateThumbnail(masterFilePath, outputFilePath);
    }

    public static void generateThumbnail(String masterFilePath, String outputThumbnailFilePath) throws Exception {
        float totalDurationSec = Float.parseFloat(getContentDurationSec(masterFilePath));
        int startTimeSec = Math.max(1, (int) (THUMBNAIL_RATIO_OF_TOTAL_CONTENT * totalDurationSec));
        String startTimeFormatted = secondsToTime(startTimeSec);
        log.info("startTimeFormatted:{}", startTimeFormatted);

        String[] cmd = new String[]{"-ss", startTimeFormatted, "-i", masterFilePath,
                "-vf", "scale=1280:-1", "-vframes",
                "1", outputThumbnailFilePath};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    private static String secondsToTime(int sec) {
        Date d = new Date(sec * 1000L);
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(d);
    }

    private static String getContentDurationSec(String masterFilePath) throws Exception {
        String ffprobe = Loader.load(org.bytedeco.ffmpeg.ffprobe.class);
        String[] cmd = new String[]{
                "-v", "error", "-show_entries", "format=duration", "-of",
                "default=noprint_wrappers=1:nokey=1", masterFilePath
        };

        List<String> finalInputs = new ArrayList<>();
        finalInputs.add(ffprobe);
        finalInputs.addAll(Arrays.asList(cmd));
        ProcessBuilder pb = new ProcessBuilder(finalInputs);
        File logFile = File.createTempFile(String.format("%s-%s", "contentEncoderLog", UUID.randomUUID()), ".log");
        try {
            logFile.deleteOnExit();
//        pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);

            int exitVal = pb.start().waitFor();
            Preconditions.checkState(exitVal == 0, "process failed:" + masterFilePath);

            try (Stream<String> s = Files.lines(logFile.toPath())) {
                List<String> lines = s.toList();
                log.info("lines for duration:" + lines);
                Preconditions.checkState(lines.size() == 1, "some error in log file");
                return lines.get(0);
            }
        } finally {
            logFile.delete();
        }
    }

    private static void updateViaFFMPEG(List<String> inputs) throws IOException, InterruptedException {
        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        List<String> finalInputs = new ArrayList<>();
        finalInputs.add(ffmpeg);
        finalInputs.addAll(inputs);
        // scale=-2:480 maintains the same aspect ration as original video
        ProcessBuilder pb = new ProcessBuilder(finalInputs);

        int exitVal = pb.inheritIO().start().waitFor();
        Preconditions.checkState(exitVal == 0, "process failed:" + inputs);
    }

    private static String getHLSFilePath(String inputFile, String outputFileDir) {
        return getFilePath(inputFile, outputFileDir, "m3u8");
    }

    private static String getFilePath(String inputFile, String outputFileDir, String suffix) {
        File file = new File(inputFile);
        String outputFileNamePrefix = file.getName().substring(0, file.getName().lastIndexOf("."));
        String outputFileName = String.format("%s.%s", outputFileNamePrefix, suffix);
        return String.format("%s/%s", outputFileDir, outputFileName);
    }
}
