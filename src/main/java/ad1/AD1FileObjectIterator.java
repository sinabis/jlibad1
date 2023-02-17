package ad1;

import ad1.content.AD1FileObject;
import ad1.content.AD1FileStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.zip.Inflater;

public class AD1FileObjectIterator implements Iterator<byte[]> {
    private final AD1FileStream ad1FileStream;
    private final ByteBuffer buffer;
    private long[] fencePostPositions;
    private final Inflater inflater = new Inflater();
    private final int blockSize;
    private int sliceCounter = 1;

    public AD1FileObjectIterator(AD1FileObject ad1FileObject, AD1FileStream ad1FileStream) {
        this.ad1FileStream = ad1FileStream;
        this.blockSize = 64 * 1024;
        this.buffer = ByteBuffer.allocate(this.blockSize);

        init(ad1FileObject);
    }

    public AD1FileObjectIterator(AD1FileObject ad1FileObject, AD1FileStream ad1FileStream, int blockSize) {
        this.ad1FileStream = ad1FileStream;
        this.blockSize = blockSize;
        this.buffer = ByteBuffer.allocate(this.blockSize);

        init(ad1FileObject);
    }

    private void init(AD1FileObject ad1FileObject) {
        int chunkCount = ad1FileObject.getFencePostCount();
        this.fencePostPositions = new long[chunkCount];

        ByteBuffer fencePostPositionsBuffer;

        if (ad1FileObject.getFileSize() > 0 & chunkCount > 0) {
            try {
                this.ad1FileStream.seek(ad1FileObject.getFirstFencePost());
                fencePostPositionsBuffer = ByteBuffer.wrap(this.ad1FileStream.readNBytes(chunkCount * Long.BYTES)).order(ByteOrder.LITTLE_ENDIAN);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < chunkCount; i++) {
                this.fencePostPositions[i] = fencePostPositionsBuffer.getLong(i*8);
            }

            try {
                this.ad1FileStream.seek(fencePostPositions[0]);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (this.fencePostPositions.length == 0) {
            return false;
        }

        return this.sliceCounter < fencePostPositions.length;
    }

    @Override
    public byte[] next() {
        long start = this.fencePostPositions[this.sliceCounter - 1];
        long end = this.fencePostPositions[this.sliceCounter];

        this.sliceCounter++;

        byte[] buffer;

        try {
            int size = Math.toIntExact(end - start);
            buffer = new byte[size];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            this.ad1FileStream.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return decompress(buffer);
    }

    private byte[] decompress(byte[] data) {
        try {
            this.inflater.setInput(data);
            this.inflater.inflate(buffer);
            inflater.reset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] outData = new byte[buffer.position()];
        buffer.rewind()
                .get(outData)
                .clear();

        return outData;
    }
}