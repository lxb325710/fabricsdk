package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.domain.SampleStore;
import com.demo.fabric.domain.SampleUser;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 用户身份证书管理
 */
public class CertService {

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

    private SampleStore sampleStore;

    @PostConstruct
    public void init(){
        sampleStore = new SampleStore(keystore);
    }

    /**
     * 注册用户身份，同时生成证书。
     * @param userVO
     * @throws Exception
     */
    public Enrollment registerIdentityAndGenCert(UserVO userVO)throws Exception{

        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(userVO.getOrganization());
        // 正式生产环境可能需要从 database 或者ldap 中检查是否已经注册.
        // 这里简单使用文件存储
        SampleUser user = sampleStore.getMember(userVO.getName(),userVO.getOrganization());
        if (!user.isRegistered()) {
            RegistrationRequest rr = new RegistrationRequest(userVO.getName(),userVO.getAffiliation());
            rr.setSecret(userVO.getEnrollmentSecret());
            userVO.getAttributes()
                    .stream()
                    .forEach(f->{
                        rr.addAttribute(f);
                    });
            user.setEnrollmentSecret(hfcaClient.register(rr, admin));
        }
        //注册完成后，拉取用户证书
        if (!user.isEnrolled()) {
            user.setEnrollment(hfcaClient.enroll(user.getName(), user.getEnrollmentSecret()));
            user.setMspId(userVO.getMspId());
        }

        return user.getEnrollment();
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
        SampleUser user = sampleStore.getMember(userName,orgName);
        if(user!=null){
            user.setEnrollment(hfcaClient.reenroll(user));
            return user.getEnrollment();
        }
        return null;
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
