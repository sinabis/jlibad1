
# jlibad1 &middot; [![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/sinabis/jlibad1/blob/main/LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/sinabis/jlibad1/blob/main/pulls)
jlibad1 is library that can be used to process and extract content from AD1 containers.
With the help of the library it is also possible to process segmented AD1 containers.
(i.e. data.ad1, data.ad2, ...) Currently, the processing of 
encrypted AD1 containers is not yet supported.

## Get started
A very simple program to get started with jlibad1 may look something like this:

```java
import ad1.AD1FileObjectIterator;
import ad1.AD1MetaReader;
import ad1.content.AD1FileObject;
import ad1.content.AD1FileStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        Path imageFile = Paths.get("test_data/EVID_Sinabis_sample.ad1");
        try (AD1MetaReader ad1Container = new AD1MetaReader(imageFile.toString())) {
            try (AD1FileStream ad1FileStream = new AD1FileStream(imageFile.toString())) {
                while (ad1Container.hasNext()) {
                    AD1FileObject ad1FileObject = ad1Container.next();
                    AD1FileObjectIterator it = new AD1FileObjectIterator(ad1FileObject, ad1FileStream);
                    System.out.println("Iterating: " + ad1FileObject.getName());

                    // Write each file in container to /tmp
                    try (FileOutputStream fileOutputStream = new FileOutputStream("/tmp/" + ad1FileObject.getName())) {
                        while (it.hasNext()) {
                            try {
                                byte[] data = it.next();
                                fileOutputStream.write(data);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
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
```

## Importat notice

Please be aware that this library uses a non-markable stream internally. All segment-files are concatenated together by
using a SequenceInputStream. This makes this implementation very easy to understand. Most of the code in the
class "AD1FileStream" is only needed to ensure that all segment-files are correct and none is missing. Handling of the
stream is completely done by the java-internal implementation of SequenceInputStream. Because of this you can only extract 
files in the order of the stream. "Jumping around" in a container is currently not possible.

The implementation is robust enough to extract the content of AD1-containers consisting of multiple terabytes of data
and millions of files. You have only to ensure that you are providing enough heap memory when processing such a container.

## Issues
Find a bug or want to request a new feature? Please let us know by submitting an issue.

## Contributing
Check out our [guidelines](./CONTRIBUTING.md) on how you can contribute to the project.

## License
jlibad1 is [MIT licensed](./LICENSE).