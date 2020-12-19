package com.cswiki.rpc.registry.zookeeper;

import com.cswiki.rpc.registry.ServiceDiscovery;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 使用 ZooKeeper 实现服务发现功能
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    private String zkAddress;

    public ZooKeeperServiceDiscovery(String zkAddress) {
        this.zkAddress = zkAddress;
    }


    /**
     * 服务发现
     * @param serviceName 服务名称（方法名称）
     * @return
     */
    @Override
    public String discover(String serviceName) {
        ZkClient zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        LOGGER.info("connect zookeeper");

        try{
            // 获取 service 节点（存放的是服务名称）
            String servicePath = Constant.ZK_REGISTRY_PATH + "/" + serviceName;
            // 如果该节点不存在，则抛出异常
            if(!zkClient.exists(servicePath)){
                throw new RuntimeException(String.format("can not find any service node on path: %s\", servicePath"));
            }

            // 获取 service 节点的孩子节点 —— address 节点（存放的是服务地址）
            List<String> addressList = zkClient.getChildren(servicePath);
            // 若孩子节点为空，则抛出异常
            if(CollectionUtils.isEmpty(addressList)){
                throw new RuntimeException(String.format("can not find any address node on path: %s", servicePath));
            }

            // 获取 address 节点(一个 service 节点可能会对应多个服务地址（相同服务被部署多份的情况）)
            String address;
            int size = addressList.size();
            // 如果只有一个地址，则获取该地址
            if (size == 1){
                address = addressList.get(0);
                LOGGER.info("get only address node: {}", address);
            }
            // 若存在多个地址，则随机获取一个地址
            else {
                address = addressList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.info("get random address node: {}", address);
            }

            // 获取 address 节点的值
            String addressPath = servicePath + "/" + address;
            return zkClient.readData(addressPath);
        }
        finally {
            zkClient.close();
        }
    }
}
