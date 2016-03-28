package com.github.rutledgepaulv.creek.demuxers;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

abstract class AbstractDemux implements Supplier<InputStream> {

    private int numberOfClosedReaders;
    private int numberOfOnceActiveReaders;
    private int numberOfSpawnedReaders;
    private final InputStream source;

    protected AbstractDemux(InputStream source) {
        this.source = source;
    }

    protected InputStream getSource() {
        return source;
    }

    protected int getNumberOfClosedReaders() {
        return numberOfClosedReaders;
    }

    protected int getNumberOfOnceActiveReaders() {
        return numberOfOnceActiveReaders;
    }

    protected int getNumberOfSpawnedReaders() {
        return numberOfSpawnedReaders;
    }

    protected boolean haveAllSpawnedReadersBeenClosed() {
        return getNumberOfSpawnedReaders() == getNumberOfClosedReaders();
    }

    private void markFollowerBecameActive() {
        numberOfOnceActiveReaders++;
        if(getNumberOfOnceActiveReaders() == 1) {
            onFirstActiveInternal();
        }
    }

    private void markFollowerWasClosed() {
        numberOfClosedReaders++;
        if(haveAllSpawnedReadersBeenClosed()) {
            onLastClosedInternal();
        }
    }



    @Override
    public InputStream get() {

        if(numberOfOnceActiveReaders > 0 || numberOfClosedReaders > 0) {
            String message = "You cannot get another handle to the stream once the source is being consumed.";
            throw new IllegalStateException(message);
        }

        InputStream spawn = getNewHandle();
        numberOfSpawnedReaders++;

        return new DelegatingInputStream(spawn) {

            private boolean haveReportedActiveStatus = false;
            private boolean haveReportedClosedStatus = false;

            @Override
            public int read(byte[] b) throws IOException {
                if(!haveReportedActiveStatus) {
                    markFollowerBecameActive();
                }
                return super.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if(!haveReportedActiveStatus) {
                    markFollowerBecameActive();
                }
                return super.read(b, off, len);
            }

            @Override
            public int read() throws IOException {
                if(!haveReportedActiveStatus) {
                    markFollowerBecameActive();
                }
                return super.read();
            }

            @Override
            public void close() throws IOException {
                super.close();
                if(!haveReportedClosedStatus) {
                    markFollowerWasClosed();
                }
            }

        };

    }


    private void onFirstActiveInternal() {
        onFirstActive();
    }

    private void onLastClosedInternal() {
        IOUtils.closeQuietly(source);
        onLastClosed();
    }

    protected abstract InputStream getNewHandle();
    protected void onFirstActive() {}
    protected void onLastClosed() {}

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        IOUtils.closeQuietly(source);
    }

}
