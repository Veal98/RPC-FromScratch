package com.cswiki.codec;

import com.cswiki.serializer.CustomSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 自定义解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass; // 反序列化成 genericClass 类型的对象

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 消息头占 4B，所以 "入站"数据（待解码的字节序列）的可读字节必须大于 4
        if (in.readableBytes() < 4){
            return ;
        }
        // 标记当前readIndex的位置，以便后面重置 readIndex 的时候使用
        in.markReaderIndex();
        // 读取消息体（消息的长度）. readInt 操作会增加 readerIndex
        int dataLength = in.readInt();
        // 如果可读字节数小于消息长度，说明是不完整的消息
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return ;
        }
        // 开始反序列化
        byte[] body = new byte[dataLength];
        in.readBytes(body);
        Object obj = CustomSerializer.deserialize(body, genericClass);
        out.add(obj);
    }
}
