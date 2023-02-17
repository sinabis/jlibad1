package ad1.content;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AD1FileStream extends InputStream implements AutoCloseable {
    private static final int MAX_BUFFER_SIZE = 65 * 1024;
    private long streamOffset;
    private final AD1SegmentFile[] ad1SegmentFiles;
    private final int margin;
    private final AtomicLong calculatedFileSize;
    private InputStream ad1FileStream;

    public AD1FileStream(String ad1FirstFilename) throws IOException {
        String ad1ExtensionRegex = "\\.[aA][dD]\\d{1,5}$";
        String forbiddenChars = "[\\[\\]()]";
        this.margin = 512;
        this.calculatedFileSize = new AtomicLong();

        Pattern ad1FileExtensionPattern = Pattern.compile(".*" + ad1ExtensionRegex);
        Path firstFile = Path.of(ad1FirstFilename);

        Matcher ad1Matcher = ad1FileExtensionPattern.matcher(firstFile.toString());

        AD1SegmentFile firstAD1SegmentFile = genAD1File(firstFile);

        if (!ad1Matcher.matches() | firstAD1SegmentFile == null) {
             throw new IOException("File is not an AD1-file!");
        } else {
            Path ad1Path = firstFile.getParent();
            Path ad1FileName = firstFile.getFileName();
            String filenameWithOutExtension = ad1FileName.toString()
                    .replaceAll(ad1ExtensionRegex,"")
                    .replaceAll(forbiddenChars, "");

            Pattern ad1FilenamePattern = Pattern.compile(filenameWithOutExtension + ad1ExtensionRegex);

            int numberOfAD1Files = firstAD1SegmentFile.numberOfFiles();
            this.ad1SegmentFiles = new AD1SegmentFile[numberOfAD1Files];

            try (Stream<Path> files = Files.list(ad1Path)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    Path fullFilename = file.toAbsolutePath();
                    Path fileName = file.getFileName();
                    Matcher m = ad1FilenamePattern.matcher(fileName.toString().replaceAll(forbiddenChars, ""));

                    if (m.matches()) {
                        AD1SegmentFile ad1SegmentFile = genAD1File(fullFilename);
                        int ad1FileNumber = Objects.requireNonNull(ad1SegmentFile).fileNumber();
                        this.ad1SegmentFiles[ad1FileNumber - 1] = ad1SegmentFile;
                    }
                });
            }
        }

        createStream();
    }

    public void seek(long position) throws IOException {
        long skip = position - this.streamOffset;
        this.ad1FileStream.skip(skip);
        this.streamOffset += skip;
    }

    @Override
    public byte[] readNBytes(int size) throws IOException {
        byte[] data = this.ad1FileStream.readNBytes(size);

        if (data.length < size) {
            throw new IOException("Incomplete read!");
        }

        this.streamOffset += size;
        return data;
    }

    @Override
    public int available()  {
        long rest = this.calculatedFileSize.get() - this.streamOffset - this.margin;

        if (rest > 0) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int read() throws IOException {
        this.streamOffset += 1;
        return this.ad1FileStream.read();
    }

    @Override
    public long skip(long n) throws IOException {
        this.streamOffset += n;
        return this.ad1FileStream.skip(n);
    }

    @Override
    public void close()  {
        try {
            this.ad1FileStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getCalculatedFileSize() {
        return calculatedFileSize.get();
    }

    public long getPosition() {
        return this.streamOffset;
    }

    private void createStream()  {
        this.streamOffset = 0;
        List<InputStream> inputStreams = Arrays.stream(this.ad1SegmentFiles)
                .map(ad1Path -> {
                    try {
                        File file = ad1Path.filename().toFile();
                        long fileLength = file.length() - this.margin;
                        calculatedFileSize.addAndGet(fileLength);
                        InputStream stream = new BufferedInputStream(new FileInputStream(file), MAX_BUFFER_SIZE);
                        stream.skip(this.margin); // Skip the first 512 bytes in all files.
                        return stream;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        this.ad1FileStream = new SequenceInputStream(Collections.enumeration(inputStreams));
    }

    private AD1SegmentFile genAD1File(Path path) {
        byte[] normalHeader = {65, 68, 83, 69, 71, 77, 69, 78, 84, 69, 68, 70, 73, 76, 69, 0};

        try (FileInputStream stream = new FileInputStream(path.toString())) {
            ByteBuffer header = ByteBuffer.wrap(stream.readNBytes(512)).order(ByteOrder.LITTLE_ENDIAN);
            byte[] firstBytes = new byte[16];
            header.get(firstBytes);

            if (!compareByteArrays(firstBytes, normalHeader)) {
                return null;
            }

            int fileNumber = header.getInt(24);
            int numberOfFiles = header.getInt(28);
            return new AD1SegmentFile(path, fileNumber, numberOfFiles, false);
        } catch (IOException e) {
            return null;
        }
    }

    public void setDataSize(long attrDataSize) {
        this.calculatedFileSize.set(attrDataSize);
    }

    public static boolean compareByteArrays(byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; i++) {
            byte value1 = array1[i];
            byte value2 = array2[i];

            if (value1 != value2) {
                return false;
            }
        }

        return true;
    }
}
