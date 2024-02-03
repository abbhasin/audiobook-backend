package com.enigma.audiobook.backend.jobs;

import org.bytedeco.javacpp.Loader;

import java.io.IOException;

public class ContentEncoderV2 {

    public static void main(String[] args) throws IOException, InterruptedException {
        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        String inputFile = "/Users/akhil/Downloads/test/video2/raw/videoplayback.mp4";
        String outputFile = "/Users/akhil/Downloads/test/video2/ID-34kjbnwed34/hls/out2.m3u8";
        // scale=-2:480 maintains the same aspect ration as original video
        ProcessBuilder pb = new ProcessBuilder(
                ffmpeg, "-i", inputFile, "-vf", "scale=-2:480", "-c:v", "h264", "-b:v", "500k",
                "-c:a", "aac", "-b:a", "128k", "-hls_time", "10", "-hls_list_size", "0", outputFile);

        pb.inheritIO().start().waitFor();
    }
}
