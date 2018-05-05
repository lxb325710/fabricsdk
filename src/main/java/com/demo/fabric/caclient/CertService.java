package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.domain.SampleStore;
import com.demo.fabric.domain.SampleUser;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 用户身份证书管理
 */
@Slf4j
@Service("certService")
public class CertService {


    private  AffiliationService affiliationService;

    /**
     * 区块链服务
     */
    @Resource
    private BlockchainService blockchainService;

    /**
     * 用户身份管理服务
     */
    @Resource
    private IdentityService identityService;

    /**
     * 区块链配置服务
     */
    @Resource
    private ConfigService configService;

    @Value("${blockchain.keystore.path}")
    private String  keystore;

    /**
     * 拉取身份认证证书
     * @param userName
     * @param secret
     * @return
     * @throws Exception
     */
    public Enrollment enroll(String userName,String secret)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        return hfcaClient.enroll(userName,secret);
    }

    /**
     * 重新拉取新证书
     * @param orgName
     * @param userName
     * @return
     * @throws Exception
     */
    public Enrollment reenroll(String orgName,String userName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        HFCAIdentity identity = identityService.queryIdentity(orgName,userName);
        SampleUser user = new SampleUser(orgName,userName,null);
        user.setEnrollmentSecret(identity.getSecret());
        return hfcaClient.reenroll(user);
    }

    /**
     * 撤销证书
     * 用户身份信息需要有 hf.Revoker=true 属性
     */
    public void revokeCert(UserVO user)throws Exception {
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(user.getOrganization());
        //撤销用户证书，只有包含 hf.Revoker=true 属性的用户身份证书可以被撤销。
        hfcaClient.revoke(user, user.getEnrollment(), " revoke test");

    }
}
