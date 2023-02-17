
import ad1.AD1FileObjectIterator;
import ad1.AD1MetaReader;
import ad1.content.AD1FileObject;
import ad1.content.AD1FileStream;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAD1 {
    @Test
    public void testExtraction() {
        Path imageFile = Paths.get("test_data/EVID_Sinabis_sample.ad1");

        try (AD1MetaReader ad1Container = new AD1MetaReader(imageFile.toString())) {
            try (AD1FileStream ad1FileStream = new AD1FileStream(imageFile.toString())) {
                while (ad1Container.hasNext()) {
                    AD1FileObject ad1FileObject = ad1Container.next();

                    AD1FileObjectIterator it = new AD1FileObjectIterator(ad1FileObject, ad1FileStream);
                    while (it.hasNext()) {
                        String realSHA1 = ad1FileObject.getSHA1();
                        Hasher hasher = Hashing.sha1().newHasher();

                        while (it.hasNext()) {
                            try {
                                byte[] data = it.next();
                                hasher.putBytes(data);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        assertEquals(realSHA1, hasher.hash().toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
