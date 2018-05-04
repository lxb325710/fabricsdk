package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.domain.SampleStore;
import com.demo.fabric.domain.SampleUser;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;

/**
 * blockchain 用户身份管理服务
 */
@Service("userService")
public class IdentityService {

    @Resource
    private BlockchainService blockchainService;

    @Value("${blockchain.keystore.path}")
    private String  keystore;

    private SampleStore sampleStore;

    @Resource
    private ConfigService configService;

    @PostConstruct
    public void init(){
        sampleStore = new SampleStore(keystore);
    }

    /**
     * 注册用户身份信息
     */
    public Enrollment registerIdentity(UserVO userVO) throws Exception{

        HFCAClient hfcaClient = blockchainService.getCa();
        HFCAIdentity identity = hfcaClient.newHFCAIdentity(userVO.getName());
        // 正式生产环境可能需要从 database 或者ldap 中检查是否已经注册.
        // 这里简单使用文件存储
        SampleUser user = sampleStore.getMember(userVO.getName(),userVO.getOrganization());
        if (!user.isRegistered()) {
            identity.setAffiliation(userVO.getAffiliation());
            identity.setMaxEnrollments(-1);
            identity.setSecret(userVO.getEnrollmentSecret());
            identity.setType("user");
            int statusCode = identity.create(blockchainService.getClient().getUserContext());
            if(HttpStatus.CREATED.value()!=statusCode){
                return null;
            }
            user.setEnrollmentSecret(userVO.getEnrollmentSecret());
        }
        //注册完成后，拉取用户证书
        user.setEnrollment(blockchainService.getCa().enroll(user.getName(), user.getEnrollmentSecret()));
        NetworkConfig.OrgInfo orgInfo = configService.getBlockchainConfig().getOrganizationInfo(userVO.getOrganization());
        user.setMspId(orgInfo.getMspId());
        return user.getEnrollment();
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
