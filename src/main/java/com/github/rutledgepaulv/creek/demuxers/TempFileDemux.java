package com.github.rutledgepaulv.creek.demuxers;

import org.apache.commons.io.IOUtils;

import java.io.*;

import static java.io.File.createTempFile;

/**
 * An input stream demux driven by a temporary file. Data is written
 * to the temporary file upon first read and each generated handle is
 * backed by this file.
 *
 * Note that responsibility of available disk space and permissions is
 * outside the scope of what this library provides. It may be dangerous
 * to write input streams to a temporary file that could exceed the available
 * disk space on the machine.
 *
 */
public class TempFileDemux extends AbstractInputStreamDemux<FileInputStream> {

    private final File output;

    public TempFileDemux(InputStream source) {
        super(source);
        try {
            output = createTempFile(getClass().getSimpleName(), ".data");
            output.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected FileInputStream getNewHandle() {
        try {
            return new FileInputStream(output);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onFirstHandleActivated(FileInputStream inputStream) {
        try (FileOutputStream outputStream = new FileOutputStream(output)) {
            IOUtils.copy(getSource(), outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(getSource());
        }
    }

    @Override
    protected void onLastHandleClosed(FileInputStream inputStream) {
        //noinspection ResultOfMethodCallIgnored
        output.delete();
    }

}
