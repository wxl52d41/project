package com.wxl52d41.apisign.starter.web;

import com.wxl52d41.apisign.core.SignErrorCode;
import com.wxl52d41.apisign.core.SignException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 可重复读取 body 的 RequestWrapper
 *
 * 构造时即把 body 缓存为字节数组，之后每次 getInputStream / getReader 都返回新的流，
 * 保证"验签读一次 + @RequestBody 再读一次"都能拿到完整 body。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

    /**
     * body 缓存的最大字节数，超过则直接拒绝（防止超大 JSON 请求耗尽内存）
     */
    private static final int MAX_BODY_SIZE = 2 * 1024 * 1024;

    /**
     * body 缓存
     */
    private final byte[] body;

    public RepeatableReadRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 构造时即缓存，避免流在包装前被消费
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int n;
        while ((n = request.getInputStream().read(buffer)) != -1) {
            total += n;
            if (total > MAX_BODY_SIZE) {
                throw new SignException("请求体超过允许的大小：" + MAX_BODY_SIZE + " 字节", SignErrorCode.CODE_12206);
            }
            baos.write(buffer, 0, n);
        }
        this.body = baos.toByteArray();
    }

    /**
     * 获取 body 缓存（调试 / 日志用）
     *
     * @return body 字节数组
     */
    public byte[] getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        // 每次返回全新的流，保证可重复读
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return bais.read(b, off, len);
            }

            @Override
            public long skip(long n) {
                return bais.skip(n);
            }

            @Override
            public int available() {
                return bais.available();
            }

            @Override
            public void close() {
                // 底层是内存流，关闭无副作用
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding == null ? Charset.forName("UTF-8") : Charset.forName(encoding);
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
