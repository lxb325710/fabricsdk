package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.domain.SampleUser;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * 用户身份证书管理
 */
@Slf4j
@Service("certService")
public class CertService {

    /**
     * 区块链服务
     */
    @Resource
    private BlockchainService blockchainService;

    @Resource
    private ConfigService configService;

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
    public Enrollment reenroll(String orgName,String userName,Enrollment enrollment)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        SampleUser user = new SampleUser(userName,orgName);
        user.setEnrollment(enrollment);
        return hfcaClient.reenroll(user);
    }

    /**
     * 撤销证书
     * 用户身份信息需要有 hf.Revoker=true 属性
     */
    public void revokeCert(UserVO user,Enrollment enrollment)throws Exception {
        HFCAClient hfcaClient = blockchainService.getCa();
        //撤销用户证书，只有包含 hf.Revoker=true 属性的用户身份证书可以被撤销。
        hfcaClient.revoke(user, user.getEnrollment(), " revoke test");
    }

    public void generateCRL(String orgName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        Calendar dateBefore= Calendar.getInstance();
        dateBefore.set(2058,0,1,23,59,59);
        System.out.println(dateBefore.toString());

        Calendar dateAfter= Calendar.getInstance();
        dateAfter.set(2018,0,1,0,0,0);
        System.out.println(dateAfter.toString());
        String crllist = hfcaClient.generateCRL(admin,dateBefore.getTime(),dateAfter.getTime(),dateBefore.getTime(),dateAfter.getTime());
        System.out.println(crllist);
    }
}
