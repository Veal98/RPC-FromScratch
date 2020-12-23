package com.cswiki.rpc.registry.zookeeper;

/**
 * 定义 Zookeeper 使用过程中会用到的一些常量
 */
public interface Constant {

    int ZK_SESSION_TIMEOUT = 5000;
    int ZK_CONNECTION_TIMEOUT = 1000;

    // 在该节点下存放所有的服务节点
    String ZK_REGISTRY_PATH = "/registry";
}
