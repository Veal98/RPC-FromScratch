package com.cswiki.rpc.common.codec;

import com.cswiki.rpc.common.serialize.KryoSerializer;
import com.cswiki.rpc.common.serialize.ProtostuffSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码器
 */
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass; // 代编码的对象

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 编码
     * @param channelHandlerContext
     * @param in 待编码的对象（待发送的消息）
     * @param out 将编码后的字节序列存入 ByteBuf
     * @throws Exception
     */
    @Override
    public void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)){
            byte[] data = ProtostuffSerializer.serialize(in); // 将对象序列化为字节数组
            out.writeInt(data.length); // 将消息体长度写入消息头
            out.writeBytes(data); // 将数据写入消息体
        }
    }
}
