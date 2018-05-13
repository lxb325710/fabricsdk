package com.demo.fabric.blockchain;

import com.demo.fabric.domain.SampleUser;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import lombok.Getter;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 加载 fabri-sdk client 配置
 */
@Component
public class ConfigService {

    @Getter
    private NetworkConfig blockchainConfig;

    @Value("${blockchain.conf.path}")
    private String fabricConfigPath;

    @Value("${blockchain.user1.privateKeyFile}")
    private String user1PrivateKeyFile;

    @Value("${blockchain.user1.certificateFile}")
    private String user1CertificateFile;

    @Value("${blockchain.ordererAdmin.privateKeyFile}")
    private String ordererAdminPrivateKeyFile;

    @Value("${blockchain.ordererAdmin.certificateFile}")
    private String ordererAdminCertificateFile;

    @Getter
    @Value("${blockchain.configtxlator.location}")
    private String configtxlatorLocation;

    @Getter
    private SampleUser user;

    @Getter
    private SampleUser ordererAdmin;

    /**
     * 连接区块链网络客户端
     */
    @Getter
    private HFClient client;

    /**
     * 区块链 MSP 用户身份服务提供商 客户端
     */
    @Getter
    private HFCAClient ca;

    @PostConstruct
    protected void loadConfig() throws Throwable {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        /**
         * 这里从json 格式文件中获取配置
         * yaml 格式类似.这里不再演示
         */
        try(InputStream intStream = Files.newInputStream(Paths.get(fabricConfigPath))){
            blockchainConfig = NetworkConfig.fromYamlStream(intStream);
        }

        initCAClient();
        initClient();

        loadUser();
    }

    /**
     *  初始化CAClient
     * @throws Throwable
     */
    private void initCAClient() throws Throwable{

        /**
         * CA Server 可以使用 kubernetes service 做负载均衡。这里只取第一个
         */
        List<NetworkConfig.CAInfo> CAInfos = blockchainConfig.getClientOrganization().getCertificateAuthorities();
        if(!CAInfos.isEmpty()){
            ca = HFCAClient.createNewInstance(CAInfos.get(0));
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            HFCAInfo info = ca.info(); //just check if we connect at all.
            assertNotNull(info);
            String infoName = info.getCAName();
            if (infoName != null && !infoName.isEmpty()) {
                assertEquals(ca.getCAName(), infoName);
            }
        }
    }

    /**
     * 初始化区块链client
     * @throws Throwable
     */
    private void initClient() throws Throwable{
        NetworkConfig.UserInfo userInfo = blockchainConfig.getPeerAdmin();
        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        // 默认为peerAdmin
        client.setUserContext(userInfo);
    }

    private void loadUser() throws Exception{

        String user1Certificate = new String(IOUtils.toByteArray(new FileInputStream(user1CertificateFile)), "UTF-8");
        PrivateKey user1PrivateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(user1PrivateKeyFile)));
        user = new SampleUser("User1","Org1");
        user.setMspId(blockchainConfig.getClientOrganization().getMspId());
        user.setEnrollment(new SampleStoreEnrollement(user1PrivateKey,user1Certificate));

        String ordererAdminCertificate = new String(IOUtils.toByteArray(new FileInputStream(ordererAdminCertificateFile)), "UTF-8");
        PrivateKey ordererAdminPrivateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(ordererAdminPrivateKeyFile)));
        ordererAdmin = new SampleUser("Admin","OrdererOrg");
        ordererAdmin.setMspId("OrdererMSP");
        ordererAdmin.setEnrollment(new SampleStoreEnrollement(ordererAdminPrivateKey, ordererAdminCertificate));
    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

    static final class SampleStoreEnrollement implements Enrollment, Serializable {

        private static final long serialVersionUID = -2784835212445309006L;
        private final PrivateKey privateKey;
        private final String certificate;

        SampleStoreEnrollement(PrivateKey privateKey, String certificate) {

            this.certificate = certificate;

            this.privateKey = privateKey;
        }

        @Override
        public PrivateKey getKey() {

            return privateKey;
        }

        @Override
        public String getCert() {
            return certificate;
        }

    }
}
