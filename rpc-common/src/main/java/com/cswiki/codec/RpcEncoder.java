package com.cswiki.codec;

import com.cswiki.entity.RpcRequest;
import com.cswiki.serializer.CustomSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义编码器
 */
public class RpcEncoder extends MessageToByteEncoder {

    private  Class<?> genericClass; // 待编码的对象类型

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            byte[] data = CustomSerializer.serialize(in); // 将对象序列化为字节数组
            out.writeInt(data.length); // 将消息体长度写入消息头
            out.writeBytes(data); // 将数据写入消息体
        }
    }
}
