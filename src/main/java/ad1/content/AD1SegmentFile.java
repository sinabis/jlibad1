package ad1.content;

import java.nio.file.Path;

public record AD1SegmentFile(Path filename, int fileNumber, int numberOfFiles,
                             @SuppressWarnings("unused") boolean encrypted) {
}
