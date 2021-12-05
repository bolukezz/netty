/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;

import java.util.List;

/**
 * A decoder that splits the received {@link ByteBuf}s on line endings.
 * <p>
 * Both {@code "\n"} and {@code "\r\n"} are handled.
 * <p>
 * The byte stream is expected to be in UTF-8 character encoding or ASCII. The current implementation
 * uses direct {@code byte} to {@code char} cast and then compares that {@code char} to a few low range
 * ASCII characters like {@code '\n'} or {@code '\r'}. UTF-8 is not using low range [0..0x7F]
 * byte values for multibyte codepoint representations therefore fully supported by this implementation.
 * <p>
 * For a more general delimiter-based decoder, see {@link DelimiterBasedFrameDecoder}.
 */
public class LineBasedFrameDecoder extends ByteToMessageDecoder {

    /** Maximum length of a frame we're willing to decode.  */
    //数据包的最大长度，超过该长度会进行丢弃模式
    private final int maxLength;
    /** Whether or not to throw an exception as soon as we exceed maxLength. */
    //超出最大长度是否要抛出异常
    private final boolean failFast;
    //最终解析的数据包是否带有换行符
    private final boolean stripDelimiter;

    /** True if we're discarding input because we're already over maxLength.  */
    //丢弃了多少字节
    private boolean discarding;
    private int discardedBytes;

    /** Last scan position. */
    private int offset;

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     */
    public LineBasedFrameDecoder(final int maxLength) {
        this(maxLength, true, false);
    }

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     * @param stripDelimiter  whether the decoded frame should strip out the
     *                        delimiter or not
     * @param failFast  If <tt>true</tt>, a {@link TooLongFrameException} is
     *                  thrown as soon as the decoder notices the length of the
     *                  frame will exceed <tt>maxFrameLength</tt> regardless of
     *                  whether the entire frame has been read.
     *                  If <tt>false</tt>, a {@link TooLongFrameException} is
     *                  thrown after the entire frame that exceeds
     *                  <tt>maxFrameLength</tt> has been read.
     */
    public LineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast) {
        this.maxLength = maxLength;
        this.failFast = failFast;
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //调用重载方法
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param   ctx             the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param   buffer          the {@link ByteBuf} from which to read data
     * @return  frame           the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                          be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        //找这行的结尾
        final int eol = findEndOfLine(buffer);
        if (!discarding) {
            if (eol >= 0) {
                final ByteBuf frame;
                //计算从换行符到可读字节之间的长度
                final int length = eol - buffer.readerIndex();
                //获得分隔符长度，如果是\r\n结尾，分隔符长度为2
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;

                //如果长度是大于最大长度
                if (length > maxLength) {
                    //指向换行符之后的可读字节(这段数据完全丢弃)
                    buffer.readerIndex(eol + delimLength);
                    //传播异常事件
                    fail(ctx, length);
                    return null;
                }

                //如果这次解析的数据是有效的
                //分隔符是否算在完整数据包里
                //true为丢弃分隔符
                if (stripDelimiter) {
                    //截取有效长度
                    frame = buffer.readRetainedSlice(length);
                    //跳过分隔符的字节
                    buffer.skipBytes(delimLength);
                } else {
                    //包含分隔符
                    frame = buffer.readRetainedSlice(length + delimLength);
                }

                return frame;
            } else {
                //如果没找到分隔符（非丢弃模式）
                //可读字节长度
                final int length = buffer.readableBytes();
                //如果超过能解析的最大长度
                if (length > maxLength) {
                    //将当前长度标记为可丢弃的
                    discardedBytes = length;
                    //直接将读指针移动到写指针
                    buffer.readerIndex(buffer.writerIndex());
                    //标记为丢弃模式
                    discarding = true;
                    offset = 0;
                    //超过最大长度抛出异常
                    if (failFast) {
                        fail(ctx, "over " + discardedBytes);
                    }
                }
                //没有超过，则直接返回
                return null;
            }
        } else {
            //丢弃模式
            if (eol >= 0) {
                //找到分隔符
                //当前丢弃的字节（前面已经丢弃的+现在丢弃的位置-写指针）
                final int length = discardedBytes + eol - buffer.readerIndex();
                //当前换行符长度为多少
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;
                //读指针直接移到换行符+换行符的长度
                buffer.readerIndex(eol + delimLength);
                //当前丢弃的字节为0
                discardedBytes = 0;
                //设置为未丢弃模式
                discarding = false;
                //丢弃完字节之后触发异常
                if (!failFast) {
                    fail(ctx, length);
                }
            } else {
                //累计已丢弃的字节个数+当前可读的长度
                discardedBytes += buffer.readableBytes();
                //移动
                buffer.readerIndex(buffer.writerIndex());
                // We skip everything in the buffer, we need to set the offset to 0 again.
                offset = 0;
            }
            return null;
        }
    }

    private void fail(final ChannelHandlerContext ctx, int length) {
        fail(ctx, String.valueOf(length));
    }

    private void fail(final ChannelHandlerContext ctx, String length) {
        ctx.fireExceptionCaught(
                new TooLongFrameException(
                        "frame length (" + length + ") exceeds the allowed maximum (" + maxLength + ')'));
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private int findEndOfLine(final ByteBuf buffer) {
        int totalLength = buffer.readableBytes();
        int i = buffer.forEachByte(buffer.readerIndex() + offset, totalLength - offset, ByteProcessor.FIND_LF);
        if (i >= 0) {
            offset = 0;
            if (i > 0 && buffer.getByte(i - 1) == '\r') {
                i--;
            }
        } else {
            offset = totalLength;
        }
        return i;
    }
}
