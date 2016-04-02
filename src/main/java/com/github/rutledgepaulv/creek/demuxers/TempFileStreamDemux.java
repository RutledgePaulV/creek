package com.github.rutledgepaulv.creek.demuxers;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Objects;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TempFileStreamDemux extends AbstractDemux {

    private File output;

    public TempFileStreamDemux(InputStream source) {
        super(source);
        setupTemporaryFile();
    }

    private void setupTemporaryFile() {
        try {
            output = File.createTempFile(getClass().getSimpleName(),".data");
            output.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected InputStream getNewHandle() {
        try {
            return new FileInputStream(output);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void onFirstActive() {
        try(FileOutputStream outputStream = new FileOutputStream(output)) {
            IOUtils.copy(getSource(), outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void onLastClosed() {
        if(Objects.nonNull(output)) {
            output.delete();
            output = null;
        }
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        onLastClosed();
    }

}
