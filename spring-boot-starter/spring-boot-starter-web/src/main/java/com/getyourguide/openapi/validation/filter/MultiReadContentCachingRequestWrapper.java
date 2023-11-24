package com.getyourguide.openapi.validation.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class MultiReadContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    public MultiReadContentCachingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public MultiReadContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
        super(request, contentCacheLimit);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        var inputStream = super.getInputStream();
        if (inputStream.isFinished()) {
            return new CachedServletInputStream(getContentAsByteArray());
        }

        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        public CachedServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() throws IOException {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
