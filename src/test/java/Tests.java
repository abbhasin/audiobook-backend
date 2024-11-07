import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;
import org.apache.http.client.utils.DateUtils;
import org.junit.Test;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class Tests {

    public static void main(String[] args) throws ParseException {
        String[] acceptedFormats = {"yyyy-MM-dd'T'HH:mm:ss.SSSX","yyyy-MM-dd'T'HH:mm:ssX","yyyy-MM-dd'T'HH:mm:ss.SSSXXX"};
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Date d1 = DateUtils.parseDate("2024-04-01T02:04:14Z", acceptedFormats);
        System.out.println("zzz date is:" + d1);
        Date d3 = DateUtils.parseDate("2024-04-01T02:04:14.000+00:00", acceptedFormats);
        System.out.println("zzz date is:" + d3);

        URI uri = URI.create("https://one-god-dev.s3.ap-south-1.amazonaws.com/dir1/dir2/objectKey.mp4");
        System.out.println(uri);
        System.out.println(uri.getAuthority());
        System.out.println(uri.getPath());
        System.out.println(uri.getHost());

        String objectKey = uri.getPath().substring(1); // remove the prefix '/'

        int firstDotIndex = uri.getAuthority().indexOf(".");
        String bucket = uri.getAuthority();
        if (firstDotIndex != -1) {
            bucket = uri.getAuthority().substring(0, firstDotIndex);
        }

        System.out.println(bucket);
        System.out.println(objectKey);

        God god = new God();
        god.setGodId("12354kjbsdfkj");
        god.setDescription("test");
        SerDe serDe = new SerDe();
        String godStr = serDe.toJson(god);
        System.out.println(godStr);
        System.out.println(serDe.fromJson(godStr, God.class));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        Date d = formatter.parse("2024-02-06T11:02:49.379+00:00");
        System.out.println(d);

        Date d2 = formatter.parse("2024-02-06T11:02:49.379Z");
        System.out.println(d2);

        String post = "{\n" +
                "    \"createTime\": \"2024-02-06T11:02:49.379+00:00\",\n" +
                "    \"updateTime\": \"2024-02-06T11:06:00.416+00:00\",\n" +
                "    \"associatedMandirId\": \"65c247231298c936bf93450b\",\n" +
                "    \"associatedInfluencerId\": null,\n" +
                "    \"associatedGodId\": null,\n" +
                "    \"fromUserId\": \"65a7936792bb9e2f44a1ea47\",\n" +
                "    \"tag\": \"SHIVA\",\n" +
                "    \"associationType\": \"MANDIR\",\n" +
                "    \"type\": \"VIDEO\",\n" +
                "    \"title\": \"Shiva ji ki pooja ka tarika\",\n" +
                "    \"description\": \"Ye video mai shiva ji ki pooja ka tarika dekhiye\",\n" +
                "    \"thumbnailUrl\": null,\n" +
                "    \"videoUrl\": \"https://one-god-dev.s3.ap-south-1.amazonaws.com/posts/videos/user/65a7936792bb9e2f44a1ea47/video/65c25f30b0ba6251a747e6ee/raw/c2hpdmFfcG9vamFfdmlkZW8=.mp4\",\n" +
                "    \"imagesUrl\": null,\n" +
                "    \"audioUrl\": null,\n" +
                "    \"contentUploadStatus\": \"RAW_UPLOADED\",\n" +
                "    \"quality\": null,\n" +
                "    \"_id\": \"65c25f30b0ba6251a747e6ee\"\n" +
                "}";
        Post p = serDe.fromJson(post, Post.class);
        System.out.println("zzz post:" + p);
        String pStr = serDe.toJson(p);
        System.out.println(pStr);
        System.out.println(serDe.fromJson(pStr, Post.class));
    }


    static ExecutorService executor1 = Executors.newFixedThreadPool(2);
    static ExecutorService executor2 = Executors.newFixedThreadPool(2);

    public static void main2(String[] args) throws InterruptedException {


        Future<?> f1 = executor1.submit(() -> {
            Future<?> f2 = executor2.submit(new TestRunnable());
            Future<?> f3 = executor2.submit(new TestRunnable());
            try {
                System.out.println("f2 get");
                f2.get();
            } catch (InterruptedException e) {
                System.out.println("f2 interrupted");
//                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                System.out.println("exception f2");
                throw new RuntimeException(e);
            }
            try {
                System.out.println("f3 get");
                f3.get();
            } catch (InterruptedException e) {
                System.out.println("f3 interrupted");
//                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                System.out.println("f3 exception");
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(5 * 1000);
        f1.cancel(true);
        while (true) {
            if (f1.isCancelled()) {
                System.out.println("first thread interrupted");
                return;
            }
            if (f1.isDone()) {
                System.out.println("first thread is done");
                return;
            }
        }
    }

    static Semaphore semaphore = new Semaphore(0);

    public static class TestRunnable implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("executing");
                semaphore.acquire();
                System.out.println("executing zzz");
            } catch (InterruptedException e) {
                System.out.println("test runnable interrupted");
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void test1() {
        System.out.println("hello world");
    }

    @Test
    public void test2() {
        throw new RuntimeException("test exception");
    }
}
