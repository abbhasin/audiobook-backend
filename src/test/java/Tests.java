import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.utils.SerDe;

import java.net.URI;

public class Tests {

    public static void main(String[] args) {
        URI uri  = URI.create("https://one-god-dev.s3.ap-south-1.amazonaws.com/dir1/dir2/objectKey.mp4");
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
    }
}
