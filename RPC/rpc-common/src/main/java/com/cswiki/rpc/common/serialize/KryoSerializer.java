package com.cswiki.rpc.common.serialize;


import com.cswiki.rpc.common.entity.RpcRequest;
import com.cswiki.rpc.common.entity.RpcResponse;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


/**
 * Kryo 实现序列化/反序列化
 */
public class KryoSerializer{

    /**
     * 由于 Kryo 不是线程安全的。每个线程都应该有自己的 Kryo，Input 或 Output 实例。
     * 所以，使用 ThreadLocal 存放 Kryo 对象
     * 这样减少了每次使用都实例化一次 Kryo 的开销又可以保证其线程安全
     */
    private static final ThreadLocal<Kryo> KryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

    /**
     * 序列化
     * @param obj 要序列化的对象
     * @return
     */
    public static byte[] serialize(Object obj) {
        try{
            Kryo kryo = KryoThreadLocal.get();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream);
            // Object->byte: 将对象序列化为 byte 数组
            kryo.writeObject(output, obj);
            KryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("serialize failed");
        }
    }

    /**
     * 反序列化
     * @param bytes 序列化后的字节数组
     * @param clazz 类
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            Kryo kryo = KryoThreadLocal.get();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            Input input = new Input(byteArrayInputStream);
            // byte->Object: 从 byte 数组中反序列化出对象
            Object o = kryo.readObject(input, clazz);
            KryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new RuntimeException("deserialize failed");
        }
    }
}
