package com.cswiki.rpc.sample.server;

import com.cswiki.rpc.sample.api.HelloService;
import com.cswiki.rpc.server.RpcService;

@RpcService(interfaceName = HelloService.class, serviceVersion = "helloServiceImpl2")
public class HelloServiceImpl2 implements HelloService {

    @Override
    public String hello(String name) {
        return name + " GoodBye from " + "HelloServiceImpl2" ;
    }
}
