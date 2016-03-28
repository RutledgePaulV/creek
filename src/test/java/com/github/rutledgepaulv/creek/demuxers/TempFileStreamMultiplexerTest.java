package com.github.rutledgepaulv.creek.demuxers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TempFileStreamMultiplexerTest {


    @Test
    public void multiplexTimes5() throws Exception {

        String streamContent = "Testing";

        Supplier<InputStream> plex = multiplexer(IOUtils.toInputStream(streamContent));

        InputStream stream1 = plex.get();
        InputStream stream2 = plex.get();
        InputStream stream3 = plex.get();
        InputStream stream4 = plex.get();
        InputStream stream5 = plex.get();


        assertEquals(streamContent, toString(stream1));
        assertEquals(streamContent, toString(stream2));
        assertEquals(streamContent, toString(stream3));
        assertEquals(streamContent, toString(stream4));
        assertEquals(streamContent, toString(stream5));

    }

    private static String toString(InputStream stream) throws IOException {
        return IOUtils.toString(stream, "UTF-8");
    }

    private static TempFileStreamDemux multiplexer(InputStream stream) {
        return new TempFileStreamDemux(stream);
    }

}
