package com.enigma.audiobook.backend.jobs;



public class ContentEncoder {

//    private static void encodeContentAndConvertToHLS(String input,
//                                                     String output,
//                                                     int hls_start,
//                                                     int hls_time,
//                                                     int hls_list_size,
//                                                     int hls_wrap,
//                                                     String hls_base_url,
//                                                     String vFilter,
//                                                     String aFilter) throws IOException, InterruptedException {
//        final Demuxer demuxer = Demuxer.make();
//
//        demuxer.open(input, null, false, true, null, null);
//
//        // we're forcing this to be HTTP Live Streaming for this demo.
//        final Muxer muxer = Muxer.make(output, null, "hls");
//        try {
//            muxer.setProperty("start_number", hls_start);
//            muxer.setProperty("hls_time", hls_time);
//            muxer.setProperty("hls_list_size", hls_list_size);
//            muxer.setProperty("hls_wrap", hls_wrap);
//            if (hls_base_url != null && hls_base_url.length() > 0)
//                muxer.setProperty("hls_base_url", hls_base_url);
//
//            final MuxerFormat format = MuxerFormat.guessFormat("mp4", null, null);
//
//            /**
//             * Create bit stream filters if we are asked to.
//             */
//            final BitStreamFilter vf = vFilter != null ? BitStreamFilter.make(vFilter) : null;
//            final BitStreamFilter af = aFilter != null ? BitStreamFilter.make(aFilter) : null;
//
//            int n = demuxer.getNumStreams();
//            final Decoder[] decoders = new Decoder[n];
//            for (int i = 0; i < n; i++) {
//                final DemuxerStream ds = demuxer.getStream(i);
//                decoders[i] = ds.getDecoder();
//                final Decoder d = decoders[i];
//
//                if (d != null) {
//                    // neat; we can decode. Now let's see if this decoder can fit into the mp4 format.
//                    if (!format.getSupportedCodecs().contains(d.getCodecID())) {
//                        throw new RuntimeException("Input filename (" + input + ") contains at least one stream with a codec not supported in the output format: " + d.toString());
//                    }
//                    if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
//                        d.setFlag(Coder.Flag.FLAG_GLOBAL_HEADER, true);
//                    d.open(null, null);
//                    muxer.addNewStream(d);
//                }
//            }
//            muxer.open(null, null);
//            final MediaPacket packet = MediaPacket.make();
//            while (demuxer.read(packet) >= 0) {
//                /**
//                 * Now we have a packet, but we can only write packets that had decoders we knew what to do with.
//                 */
//                final Decoder d = decoders[packet.getStreamIndex()];
//                if (packet.isComplete() && d != null) {
//                    // check to see if we are using bit stream filters, and if so, filter the audio
//                    // or video.
//                    if (vf != null && d.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
//                        vf.filter(packet, null);
//                    else if (af != null && d.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
//                        af.filter(packet, null);
//                    muxer.write(packet, true);
//                }
//            }
//        } finally {
//            // It is good practice to close demuxers when you're done to free
//            // up file handles. Humble will EVENTUALLY detect if nothing else
//            // references this demuxer and close it then, but get in the habit
//            // of cleaning up after yourself, and your future girlfriend/boyfriend
//            // will appreciate it.
//            muxer.close();
//            demuxer.close();
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
//            encodeContentAndConvertToHLS("/Users/akhil/Downloads/test/video/raw/MTI0REY4NEItNDk3Ri00NzZCLUEzQjktNERBQ0MzQzVCQ0Qz.mp4",
//                    "/Users/akhil/Downloads/test/video/hls/out.m3u8",
//                    0,
//                    10,
//                    0,
//                    0,
//                    null,
//                    null,
//                    null
//                    );
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
