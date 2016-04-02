package com.github.rutledgepaulv.creek.demuxers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.io.*;

import static org.junit.Assert.*;

public class TempFileDemuxTest {


    @Test
    public void multiplexTimes5() throws Exception {

        String streamContent = "Testing";

        TempFileDemux plex = multiplexer(IOUtils.toInputStream(streamContent));

        File temporary = getTemporaryFile(plex);

        assertNotNull(temporary);
        assertTrue(temporary.exists());
        assertEquals(0, temporary.length());

        InputStream stream1 = plex.get();
        InputStream stream2 = plex.get();
        InputStream stream3 = plex.get();
        InputStream stream4 = plex.get();
        InputStream stream5 = plex.get();


        assertEquals(streamContent, toString(stream1));

        assertTrue(temporary.exists());
        assertEquals(streamContent.length(), temporary.length());


        assertEquals(streamContent, toString(stream2));

        assertTrue(temporary.exists());
        assertEquals(streamContent.length(), temporary.length());


        assertEquals(streamContent, toString(stream3));

        assertTrue(temporary.exists());
        assertEquals(streamContent.length(), temporary.length());


        assertEquals(streamContent, toString(stream4));

        assertTrue(temporary.exists());
        assertEquals(streamContent.length(), temporary.length());


        assertEquals(streamContent, toString(stream5));

        assertFalse(temporary.exists());

    }

    private static String toString(InputStream stream) throws IOException {
        try {
            return IOUtils.toString(stream, "UTF-8");
        }finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private static TempFileDemux multiplexer(InputStream stream) {
        return new TempFileDemux(stream);
    }


    private static File getTemporaryFile(TempFileDemux demux) {
        return (File) Whitebox.getInternalState(demux, "output");
    }
}
