package ad1;

import ad1.content.AD1FileObject;
import ad1.content.AD1FileStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class AD1MetaReader implements Iterator<AD1FileObject>, AutoCloseable {
    private final AD1FileStream ad1FileStream;
    private final Map<Long, String> folderCache;
    private final Map<Long, Long> folderIndexCache;
    private String logicalImagePath;

    private int blockSize;
    private long lastPosition;

    public AD1MetaReader(String ad1Filename) {
        try {
            this.ad1FileStream = new AD1FileStream(ad1Filename);
            this.folderCache = new HashMap<>();
            this.folderIndexCache = new HashMap<>();
            this.logicalImagePath = "";

            this.readHeader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkContainer() {
        return crawlContainer();
    }

    @Override
    public AD1FileObject next() {
        try {
            long blockStart = this.ad1FileStream.getPosition();

            ByteBuffer buffer = ByteBuffer.wrap(this.ad1FileStream.readNBytes(5 * 8 + 2 * 4)).order(ByteOrder.LITTLE_ENDIAN);

            @SuppressWarnings({"unused"})
            long nextGroup = buffer.getLong();

            @SuppressWarnings("unused")
            long nextInGroup = buffer.getLong();
            long nextBlock = buffer.getLong();

            @SuppressWarnings("unused")
            long startOfData = buffer.getLong();
            long decompressedSize = buffer.getLong();

            int fileClass = buffer.getInt();
            int fileNameLength = buffer.getInt();

            int chunkCount = 0;
            long chunkPositionsStart = 0;

            if (decompressedSize > 0) {
                buffer = ByteBuffer.wrap(this.ad1FileStream.readNBytes(fileNameLength + 16)).order(ByteOrder.LITTLE_ENDIAN);
                chunkCount = buffer.getInt(fileNameLength + 8) + 1;
                chunkPositionsStart = this.ad1FileStream.getPosition();
                this.ad1FileStream.skip(chunkCount * 8L);
            } else {
                buffer = ByteBuffer.wrap(this.ad1FileStream.readNBytes(fileNameLength + 8)).order(ByteOrder.LITTLE_ENDIAN);
            }

            byte[] nameArray = new byte[fileNameLength];
            buffer.get(nameArray);
            String name = new String(nameArray);
            long folderIndex = buffer.getLong();

            if (fileClass == 5) {
                this.folderIndexCache.put(blockStart, folderIndex);
                this.folderCache.put(blockStart, name);
            }

            this.ad1FileStream.seek(nextBlock);

            Map<Integer, Map<Integer, String>> metaData = new HashMap<>();

            while (nextBlock > 0) {
                byte[] tmp = this.ad1FileStream.readNBytes(8 + 4 * 3);
                buffer = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN);

                nextBlock = buffer.getLong();
                int category = buffer.getInt();
                int key = buffer.getInt();
                int valueLength = buffer.getInt();

                String value = new String(this.ad1FileStream.readNBytes(Math.toIntExact(valueLength)));
                metaData.putIfAbsent(category, new HashMap<>());
                metaData.get(category).put(key, value);
            }

            this.lastPosition = this.ad1FileStream.getPosition();
            return new AD1FileObject(fileClass, folderIndex, name, metaData, chunkPositionsStart, chunkCount, this);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        return this.ad1FileStream.available() == 0 | this.lastPosition > this.ad1FileStream.getCalculatedFileSize();
    }

    private void readHeader() throws IOException {
        byte[] tmp = this.ad1FileStream.readNBytes(48);
        ByteBuffer buffer = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN);
        // AD1-version (= 3 or 4)
        long version = buffer.getInt(16);

        if (version != 3 & version != 4) {
            throw new IOException(String.format("Invalid version: %d", version));
        }

        this.blockSize = buffer.getInt(24);

        @SuppressWarnings("unused")
        long imageHeaderLength = buffer.getLong(28);
        long imageHeaderLength2 = buffer.getLong(36);
        int logicalImagePathLength = buffer.getInt(44);

        if (version == 4) {
            ByteBuffer version4Header = ByteBuffer.wrap(this.ad1FileStream.readNBytes(44)).order(ByteOrder.LITTLE_ENDIAN);
            long attrDataSize = version4Header.getLong(12);
            this.ad1FileStream.setDataSize(attrDataSize);
        }

        this.logicalImagePath = new String(this.ad1FileStream.readNBytes(logicalImagePathLength));

        if (!logicalImagePath.equals("Custom Content Image([Multi])")) {
            this.ad1FileStream.readNBytes(Math.toIntExact(imageHeaderLength2 - this.ad1FileStream.getPosition()));
        }
    }

    private boolean crawlContainer() {
        long lastPosition = 0L;

        try {
            while (this.ad1FileStream.available() == 0 | lastPosition > this.ad1FileStream.getCalculatedFileSize()) {
                ByteBuffer buffer = ByteBuffer.wrap(this.ad1FileStream.readNBytes(5 * 8 + 2 * 4)).order(ByteOrder.LITTLE_ENDIAN);

                long nextBlock = buffer.getLong(2 * Long.BYTES);
                this.ad1FileStream.seek(nextBlock);

                while (nextBlock > 0) {
                    byte[] tmp = this.ad1FileStream.readNBytes(8 + 4 * 3);
                    buffer = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN);

                    nextBlock = buffer.getLong();
                    int valueLength = buffer.getInt(8 + 2 * 4);
                    this.ad1FileStream.skip(Math.toIntExact(valueLength));
                }

                lastPosition = this.ad1FileStream.getPosition();
            }
        } catch (Exception ignored) {
            return false;
        }

        return true;
    }

    public int getBlockSize() {
        return this.blockSize;
    }

    public Map<Long, String> getFolderCache() {
        return folderCache;
    }

    public Map<Long, Long> getFolderIndexCache() {
        return folderIndexCache;
    }

    public String getLogicalImagePath() {
        return logicalImagePath;
    }

    @Override
    public void close() {
        this.ad1FileStream.close();
    }
}
