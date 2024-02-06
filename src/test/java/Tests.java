import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Tests {

    public static void main(String[] args) throws ParseException {
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
}
