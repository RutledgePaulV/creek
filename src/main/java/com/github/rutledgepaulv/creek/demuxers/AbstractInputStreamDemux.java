package com.github.rutledgepaulv.creek.demuxers;

import com.github.rutledgepaulv.creek.support.DelegatingInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class AbstractInputStreamDemux<HANDLE_KIND extends InputStream> implements Supplier<InputStream> {

    private volatile int numberOfClosedReaders;
    private volatile int numberOfOnceActiveReaders;
    private volatile int numberOfSpawnedReaders;
    private final InputStream source;

    protected AbstractInputStreamDemux(InputStream source) {
        this.source = source;
    }

    protected final InputStream getSource() {
        return source;
    }

    protected final int getNumberOfClosedReaders() {
        return numberOfClosedReaders;
    }

    protected final int getNumberOfOnceActiveReaders() {
        return numberOfOnceActiveReaders;
    }

    protected final int getNumberOfSpawnedReaders() {
        return numberOfSpawnedReaders;
    }

    protected final boolean haveAllSpawnedReadersBeenClosed() {
        return getNumberOfSpawnedReaders() == getNumberOfClosedReaders();
    }

    private synchronized void markFollowerBecameActive(HANDLE_KIND handle) {
        numberOfOnceActiveReaders++;
        if(getNumberOfOnceActiveReaders() == 1) {
            onFirstHandleActivated(handle);
        }
        onAnyHandleActivated(handle);
    }

    private synchronized void markFollowerWasClosed(HANDLE_KIND handle) {
        try {
            numberOfClosedReaders++;
            if (getNumberOfClosedReaders() == 1) {
                onFirstHandleClosed(handle);
            }
            if (haveAllSpawnedReadersBeenClosed()) {
                onLastHandleClosed(handle);
            }
            onAnyHandleClosed(handle);
        } finally {
            if(haveAllSpawnedReadersBeenClosed()) {
                IOUtils.closeQuietly(getSource());
            }
        }
    }


    private void checkAllowedToSpawn() {
        if(getNumberOfOnceActiveReaders() > 0) {
            String message = "You cannot get another handle to the stream once the source is being consumed.";
            throw new UnsupportedOperationException(message);
        }
    }


    @Override
    public synchronized final InputStream get() {

        checkAllowedToSpawn();

        final HANDLE_KIND spawn = getNewHandle();
        numberOfSpawnedReaders++;

        // proxy the stream with some eventing behaviors
        return new DelegatingInputStream(spawn) {

            private AtomicBoolean haveReportedActiveStatus = new AtomicBoolean(false);
            private AtomicBoolean haveReportedClosedStatus = new AtomicBoolean(false);

            @Override
            public int read(byte[] b) throws IOException {
                onRead();
                return super.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                onRead();
                return super.read(b, off, len);
            }

            @Override
            public int read() throws IOException {
                onRead();
                return super.read();
            }

            @Override
            public void close() throws IOException {
                super.close();
                onClose();
            }

            private void onRead() {
                if(haveReportedActiveStatus.compareAndSet(false, true)) {
                    markFollowerBecameActive(spawn);
                }
            }

            private void onClose() {
                if(haveReportedClosedStatus.compareAndSet(false, true)) {
                    markFollowerWasClosed(spawn);
                }
            }
        };

    }


    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            IOUtils.closeQuietly(source);
        }
    }


    /**
     * @return a new input stream handle that can be used to read the
     *         entire contents of the source stream eventually. The
     *         implementation of how the bytes are maintained across
     *         multiple handles is up to the implementation.
     */
    protected abstract HANDLE_KIND getNewHandle();

    /**
     * Triggered right before the first byte(s) are read from the first
     * handle to have any bytes read from it. Both this method and
     * #onAnyHandleActivated are called for the first handle.
     *
     * @param handle The handle that is being read.
     */
    protected void onFirstHandleActivated(HANDLE_KIND handle) {

    }

    /**
     * Triggered right before the first byte(s) are read from a handle.
     * Both this method and #onFirstHandleActivated are called for the
     * first handle.
     *
     * @param handle The handle that is being read.
     */
    protected void onAnyHandleActivated(HANDLE_KIND handle) {

    }

    /**
     * Triggered right after any handle has been closed.
     * This method is also called for the first and last handles
     * that are closed.
     *
     * @param handle The handle that is being closed.
     */
    protected void onAnyHandleClosed(HANDLE_KIND handle) {

    }

    /**
     * Triggered right after the first handle has been closed.
     * The onAnyHandleClosed method is also called in this case.
     *
     * @param handle The handle that is being closed.
     */
    protected void onFirstHandleClosed(HANDLE_KIND handle) {

    }

    /**
     * Triggered right after the last handle has been closed.
     * The onAnyHandleClosed method is also called in this
     * case.
     *
     * @param handle The handle that is being closed.
     */
    protected void onLastHandleClosed(HANDLE_KIND handle) {

    }

}
