package io.netty.example.my.reusebytebuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * @Author: zhudawang
 * @Date: 2021/12/04/10:39 下午
 * @Description:
 */
public class TestReUserByteBuffer {
    public static void main(String[] args) {
        final byte[] CONTENT = new byte[1024];
        int loop = 1800000;
        long startTime = System.currentTimeMillis();
        ByteBuf pollBuffer = null;
        for (int i = 0; i < loop; i++) {
            pollBuffer = PooledByteBufAllocator.DEFAULT.DEFAULT.directBuffer(1024);
            pollBuffer.writeBytes(CONTENT);
            pollBuffer.release();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("内存池分配缓冲区耗时" + (endTime - startTime) + "ms.");

        startTime = System.currentTimeMillis();
        ByteBuf buffer = null;
        for (int i = 0; i < loop; i++) {
            buffer = Unpooled.directBuffer(1024);
            buffer.writeBytes(CONTENT);
        }
         endTime = System.currentTimeMillis();
        System.out.println("非内存池分配缓冲区耗时" + (endTime - startTime) + "ms.");
    }
}
