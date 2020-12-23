package com.cswiki.rpc.sample.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 启动服务器并发布服务（其实就是加载 spring 配置文件）
 */
public class RpcBootstrap {

    public static void main(String[] args) {
        // 加载 Spring 配置文件
        new ClassPathXmlApplicationContext("spring.xml");
    }
}

