package com.cswiki.rpc.sample.client;

import com.cswiki.rpc.client.RpcProxy;
import com.cswiki.rpc.sample.api.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HelloClient {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RpcProxy rpcProxy = context.getBean(RpcProxy.class); // 获取 RpcProxy 对象
        // 调用 RpcProxy 对象的 create 方法来创建 RPC 代理接口
        HelloService helloService = rpcProxy.create(HelloService.class);
        // 调用 RPC 代理接口的方法(调用远程接口方法就像调用本地方法一样简单）
        String result = helloService.hello("World");
        System.out.println(result);

        System.exit(0);
    }
}
