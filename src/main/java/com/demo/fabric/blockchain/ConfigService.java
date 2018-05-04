package com.demo.fabric.blockchain;

import lombok.Getter;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 加载 fabri-sdk client 配置
 */
@Component
public class ConfigService {

    @Getter
    private NetworkConfig blockchainConfig;

    @Value("${blockchain.conf.path}")
    private String fabricConfigPath;

    @PostConstruct
    protected void loadConfig() throws Exception {

        /**
         * 这里从json 格式文件中获取配置
         * yaml 格式类似.这里不再演示
         */
        try(InputStream intStream = Files.newInputStream(Paths.get(fabricConfigPath));){
            blockchainConfig = NetworkConfig.fromYamlStream(intStream);
        }
    }
}
