package com.cswiki.rpc.common.codec;

import com.cswiki.rpc.common.serialize.KryoSerializer;
import com.cswiki.rpc.common.serialize.ProtostuffSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * 解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass; // 字节序列反序列化成该类型的对象

    // 调用方法示例：new RpcDecoder(RpcResponse.class)
    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 解码
     * @param channelHandlerContext 解码器关联的 ChannelHandlerContext 对象
     * @param in "入站"数据，也就是待解码的字节序列
     * @param out 解码之后的数据对象存储到 out 对象里面
     * @throws Exception
     */
    @Override
    public void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 消息头占 4B，所以 "入站"数据（待解码的字节序列）的可读字节必须大于 4
        if(in.readableBytes() < 4){
            return;
        }
        // 标记当前readIndex的位置，以便后面重置 readIndex 的时候使用
        in.markReaderIndex();
        // 读取消息体（消息的长度）. readInt 操作会增加 readerIndex
        int dataLength = in.readInt();
        // 如果可读字节数小于消息长度，说明是不完整的消息
        if(in.readableBytes() < dataLength){
            // 对于不完整的消息我们没有必要继续读下去，所以此处对 readerIndex 进行重置
            in.resetReaderIndex();
            return;
        }
        // 走到这里说明没什么问题了，可以序列化了
        byte[] body = new byte[dataLength]; // 消息体
        in.readBytes(body); // 从 in (待解码的字节序列) 中读取字节存入 body (消息体)
        // 将 bytes 数组转换为我们需要的对象
        Object obj = ProtostuffSerializer.deserialize(body, genericClass);
        out.add(obj);
    }
}
