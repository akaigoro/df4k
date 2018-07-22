package org.df4k.nio2.util;

import java.nio.charset.Charset;

public class StringEncoder {
    private final Charset charset;

    public StringEncoder() {
        this(Charset.forName("UTF-16"));
    }

    public StringEncoder(Charset charset) {
        this.charset = charset;
    }
}
