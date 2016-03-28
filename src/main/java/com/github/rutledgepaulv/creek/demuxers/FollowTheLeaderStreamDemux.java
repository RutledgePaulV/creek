package com.github.rutledgepaulv.creek.demuxers;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * An input stream demultiplexer that maintains only the delta
 * between the furthest along "leading" reader of a stream
 * and the furthest behind "following" reader of a stream.
 *
 * It's intended for usage when you have several readers that
 * each need to read through the stream but you don't want to
 * buffer to a reusable source or write to a non-volatile source.
 *
 * Note that in order to avoid loading the entire stream into the buffer,
 * you must be making progress on each fork of the stream concurrently. If
 * you're not, then you might as well load it into a byte array that you give
 * each consumer a reference to. Note that this means that this also means that
 * the size of the buffer is dependent on the rate at which each consumer is able
 * to process the stream concurrently. If they all move at the same speed, then you'll
 * find memory usage is very low. The greater the disparity in speed you'll see a larger
 * buffer get built up.
 */
public class FollowTheLeaderStreamDemux extends AbstractDemux {

    private static int MIN_BUF = 4096;
    private final List<Integer> readPositions = new ArrayList<>();
    private int[] buffer = new int[MIN_BUF];
    private int writePosition = 0;

    public FollowTheLeaderStreamDemux(InputStream source) {
        super(source);
    }

    @Override
    protected InputStream getNewHandle() {
        readPositions.add(0);
        return new InputStream() {
            private final int id = readPositions.size() - 1;
            @Override
            public int read() throws IOException {
                return FollowTheLeaderStreamDemux.this.read(id);
            }
        };
    }


    private int getReadPositionForId(int id) {
        return readPositions.get(id);
    }

    private void setReadPositionForId(int id, int value) {
        readPositions.set(id, value);
    }

    private int getLeadingReadPosition() {
        return Collections.max(readPositions);
    }

    private int getTrailingReadPosition() {
        return Collections.min(readPositions);
    }

    private void prepareBufferForMoreReadingFromSource() {
        int trailing = getTrailingReadPosition();
        int leading = getLeadingReadPosition();
        int delta = (leading - trailing);

        // guess what the new buffer should be - assume it will double
        int bufferLength = Math.max(delta * 2, MIN_BUF);

        int[] newBuf = new int[bufferLength];

        // copy the parts of the old buffer that are still necessary over to the new buffer
        System.arraycopy(buffer, trailing, newBuf, 0, delta);

        // update the read positions to be relative to the new buffer
        for (int i = 0; i < readPositions.size(); i++) {
            readPositions.set(i, readPositions.get(i) - trailing);
        }

        // update the write position to be relative to the new buffer
        writePosition -= trailing;

        // start using the new buffer
        buffer = newBuf;
    }

    private int read(int readerId) throws IOException {

        int nextPositionToRead = getReadPositionForId(readerId);

        // time to fetch more data
        if (nextPositionToRead >= writePosition) {
            prepareBufferForMoreReadingFromSource();

            // read another byte from the origin into the buffer
            buffer[writePosition++] = getSource().read();
        }

        int b = buffer[nextPositionToRead];

        if (b != -1) {
            setReadPositionForId(readerId, nextPositionToRead + 1);
        }

        return b;
    }


    @Override
    protected void onLastClosed() {
        this.buffer = new int[0];
        this.readPositions.clear();
    }

}
