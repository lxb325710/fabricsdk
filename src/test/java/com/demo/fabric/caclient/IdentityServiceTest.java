package com.demo.fabric.caclient;


import com.demo.fabric.Application;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class IdentityServiceTest {

    @Resource
    private IdentityService identityService;

    @Resource
    private ConfigService  configService;

    /**
     * 测试注册用户身份
     * @throws Exception
     */
    @Test
    public void testRegister()throws Exception{
        UserVO userVO = new UserVO("new31-user","Org1");
        userVO.setAffiliation("org1.department1");
        userVO.setMaxEnrollments(-1);
        userVO.setAccount("小明");
        userVO.setEnrollmentSecret("123456");
        List<Attribute> attrsList = new ArrayList<>();
        Attribute attrs = new Attribute("hf.Registrar.Attributes","hf.Registrar.Attributes,hf.Revoker,hf.Registrar.Roles");
        attrsList.add(attrs);
        Attribute revoker = new Attribute("hf.Revoker","true");
        attrsList.add(revoker);
        Attribute roles = new Attribute("hf.Registrar.Roles","user,client,peer");
        attrsList.add(roles);
        Attribute customAttr = new Attribute("customName","customValue",true);
        attrsList.add(customAttr);
        userVO.setAttributes(attrsList);
        Enrollment enrollment = identityService.registerIdentity(userVO);
        assertNotNull(enrollment);
        log.info(enrollment.getCert());
        log.info(new String(enrollment.getKey().getEncoded(), StandardCharsets.UTF_8));
    }

    /**
     * 测试更新用户身份
     * @throws Exception
     */
    @Test
    public void updateIdentity()throws Exception{
        int statuscode = identityService.updateIdentity("new31-user","Org1");
        assertEquals(statuscode, HttpStatus.OK.value());
    }

    /**
     * 测试查询所有用户身份
     * @throws Exception
     */
    @Test
    public void queryAllIdentity()throws Exception{
        String orgName = configService.getBlockchainConfig().getClientOrganization().getName();
        identityService.queryAllIdentity(orgName)
        .stream()
        .map(f->f.getEnrollmentId())
        .forEach(System.out::println);
    }

    /**
     * 测试查询某一个用户身份
     * @throws Exception
     */
    @Test
    public void queryIdentity()throws Exception{
        String orgName = configService.getBlockchainConfig().getClientOrganization().getName();
        HFCAIdentity identity = identityService.queryIdentity(orgName,"new31-user");
        assertNotNull(identity.getSecret());
    }

    /**
     * 删除用户身份
     * @throws Exception
     */
    @Test
    public void deleteIdentity()throws Exception{
        String orgName = configService.getBlockchainConfig().getClientOrganization().getName();
        int statusCode = identityService.deleteIdentity(orgName,"new31-user");
        assertEquals(statusCode, HttpStatus.OK.value());
    }
}
