package com.enigma.audiobook.backend.jobs;

import org.bytedeco.javacpp.Loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContentEncoderV2 {

    public static void main(String[] args) throws IOException, InterruptedException {

//        String inputFile = "/Users/akhil/Downloads/test/video2/raw/videoplayback.mp4";
//        String outputFile = "/Users/akhil/Downloads/test/video2/ID-34kjbnwed34/hls/out2.m3u8";
//
//        String inputImg = "/Users/akhil/Downloads/IMG_0728.PNG";
//        String outputImg = "/Users/akhil/Downloads/IMG_0728.JPG";
//        updateImageContent(inputImg, outputImg);

        String inputAudio = "/Users/akhil/Downloads/test/audio/raw/town-10169.mp3";
        String outputAudio = "/Users/akhil/Downloads/test/audio/ID-12345jlnfwe245/hls/town-10169.m3u8";
        updateAudioContent(inputAudio, outputAudio);

    }

    private static void updateImageContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        // scale=-2:480 maintains the same aspect ration as original video
        String[] cmd = new String[]{"-i", inputFile, "-vf", "scale=-2:480", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    private static void updateAudioContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        String[] cmd = new String[]{"-i", inputFile, "-c:a", "aac", "-b:a", "128k", "-hls_time", "10",
                "-hls_list_size", "0", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    private static void updateVideoContent(String inputFile, String outputFile) throws IOException, InterruptedException {
        // scale=-2:480 maintains the same aspect ration as original video
        String[] cmd = new String[]{"-i", inputFile, "-vf", "scale=-2:480", "-c:v", "h264", "-b:v", "500k",
                "-c:a", "aac", "-b:a", "128k", "-hls_time", "10", "-hls_list_size", "0", outputFile};
        updateViaFFMPEG(Arrays.asList(cmd));
    }

    private static void updateViaFFMPEG(List<String> inputs) throws IOException, InterruptedException {
        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        List<String> finalInputs = new ArrayList<>();
        finalInputs.add(ffmpeg);
        finalInputs.addAll(inputs);
        // scale=-2:480 maintains the same aspect ration as original video
        ProcessBuilder pb = new ProcessBuilder(finalInputs);

        pb.inheritIO().start().waitFor();
    }
}
