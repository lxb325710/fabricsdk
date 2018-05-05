package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.domain.SampleStore;
import com.demo.fabric.domain.SampleUser;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;

/**
 * blockchain 用户身份管理服务
 */
@Slf4j
@Service("identityService")
public class IdentityService {

    @Resource
    private BlockchainService blockchainService;

    @Value("${blockchain.keystore.path}")
    private String  keystore;

    @Resource
    private ConfigService configService;

    /**
     * 注册用户身份,
     * 会对登记员自身的角色权限进行检查。创建的新用户的角色权限不能大于登记员自身
     * @param userVO
     * @throws Exception
     */
    public String register(UserVO userVO)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(userVO.getOrganization());
        // 正式生产环境可能需要从 database 或者ldap 中检查是否已经注册.
        RegistrationRequest rr = new RegistrationRequest(userVO.getName(),userVO.getAffiliation());
        rr.setSecret(userVO.getEnrollmentSecret());
        rr.setType("user");
        userVO.getAttributes()
                .stream()
                .forEach(f->rr.addAttribute(f));
        //登记用户身份
        String secret = hfcaClient.register(rr,admin);
        log.debug("register username is %s, secret is %s",userVO.getName(),secret);
        return secret;
    }

    /**
     * 注册用户身份信息,使用超级管理员角色进行创建,可以创建任意类型的用户身份
     * @param userVO
     * @return
     * @throws Exception
     */
    public HFCAIdentity registerIdentity(UserVO userVO) throws Exception{

        HFCAClient hfcaClient = blockchainService.getCa();
        HFCAIdentity identity = hfcaClient.newHFCAIdentity(userVO.getName());
        // 正式生产环境可能需要从 database 或者ldap 中检查是否已经注册.
        identity.setAffiliation(userVO.getAffiliation());
        identity.setMaxEnrollments(-1);
        identity.setSecret(userVO.getEnrollmentSecret());
        identity.setType("user");
        // 创建用户身份后，并未生成证书
        int statusCode = identity.create(blockchainService.getClient().getUserContext());
        if(HttpStatus.CREATED.value()!=statusCode){
            return null;
        }
        return identity;
    }

    /**
     * 更新身份信息, 只是演示身份信息更新
     * @throws Exception
     */
    public int updateIdentity(String newUserName,String orgName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        HFCAIdentity identity = hfcaClient.newHFCAIdentity(newUserName);
        identity.setType("client");
        identity.setMaxEnrollments(100);
        return identity.update(admin);
    }

    /**
     * 查询某一个组织下，所有 用户身份
     * @param orgName
     * @return
     * @throws Exception
     */
    public Collection<HFCAIdentity> queryAllIdentity(String orgName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        // user 代表客户端身份
        return hfcaClient.getHFCAIdentities(admin);
                //.stream()
                //.map(f->f.getEnrollmentId())
                //.forEach(System.out::println); //遍历获取到的身份列表
    }

    /**
     * 查询某一个用户身份
     * @param orgName
     * @param userName
     * @return
     * @throws Exception
     */
    public HFCAIdentity queryIdentity(String orgName,String userName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        HFCAIdentity identity  = hfcaClient.newHFCAIdentity(userName);
        identity.read(admin);
        return identity;
    }

    /**
     * 删除用户身份
     * @param orgName
     * @param userName
     * @throws Exception
     */
    public int deleteIdentity(String orgName,String userName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        //构建要删除的用户身份信息对象。deleteUserName 为用户的身份id.
        HFCAIdentity identity = hfcaClient.newHFCAIdentity(userName);
        // 使用 admin 登记员身份执行删除操作。
        return identity.delete(admin);
    }
}
