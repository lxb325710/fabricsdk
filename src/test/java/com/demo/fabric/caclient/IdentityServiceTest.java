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

/**
 * 身份
 */
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
    public void testRegisterWithSuperAdmin()throws Exception{
        UserVO userVO = new UserVO("newOrg-admin","Org1");
        userVO.setAffiliation("org1.department1");
        userVO.setMaxEnrollments(-1);
        userVO.setAccount("小明");
        userVO.setEnrollmentSecret("123456");
        List<Attribute> attrsList = new ArrayList<>();
        // 添加hf.Registrar.Attributes 属性，使用新注册的用户身份添加新用户时，可以添加的用户属性列表。
        // 不在该列表中的属性，在添加新用户时，不允许添加。
        Attribute attrs = new Attribute("hf.Registrar.Attributes","*");
        attrsList.add(attrs);
        // 添加 hf.Revoker 属性，该用户的身份认证证书允许撤销
        Attribute revoker = new Attribute("hf.Revoker","true");
        attrsList.add(revoker);
        // 添加 hf.Registrar.Roles属性，如果添加该属性，可以使用新注册的用户身份添加新的用户类型。
        Attribute roles = new Attribute("hf.Registrar.Roles","client,user,peer,orderer,validator,auditor,ca");
        attrsList.add(roles);
        // 添加 hf.Registrar.DelegateRoles 属性，使用新注册的用户身份添加
        Attribute delegateRoles = new Attribute("hf.Registrar.DelegateRoles","user,client,peer");
        attrsList.add(delegateRoles);
        Attribute customAttr = new Attribute("customName","customValue",true);
        attrsList.add(customAttr);
        Attribute affiliationMgr = new Attribute("hf.AffiliationMgr","true");
        attrsList.add(affiliationMgr);

        userVO.setAttributes(attrsList);
        HFCAIdentity identity = identityService.registerIdentity(userVO);
        assertNotNull(identity);
        log.info(identity.getEnrollmentId());
    }

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
        // 添加hf.Registrar.Attributes 属性，使用新注册的用户身份添加新用户时，可以添加的用户属性列表。
        // 不在该列表中的属性，在添加新用户时，不允许添加。
        Attribute attrs = new Attribute("hf.Registrar.Attributes","hf.AffiliationMgr,hf.Registrar.Attributes,hf.Registrar.Roles,hf.Revoker,customName");
        attrsList.add(attrs);
        // 添加 hf.Revoker 属性，该用户的身份认证证书允许撤销
        Attribute revoker = new Attribute("hf.Revoker","true");
        attrsList.add(revoker);
        // 添加 hf.Registrar.Roles属性，如果添加该属性，可以使用新注册的用户身份添加新的用户类型。
        Attribute roles = new Attribute("hf.Registrar.Roles","client,user,peer");
        attrsList.add(roles);
        // 添加 hf.Registrar.DelegateRoles 属性，使用新注册的用户身份添加
        Attribute delegateRoles = new Attribute("hf.Registrar.DelegateRoles","user,client,peer");
        attrsList.add(delegateRoles);
        Attribute customAttr = new Attribute("customName","customValue",true);
        attrsList.add(customAttr);
        Attribute affiliationMgr = new Attribute("hf.AffiliationMgr","true");
        attrsList.add(affiliationMgr);

        userVO.setAttributes(attrsList);

        log.info(identityService.register(userVO));
    }

    /**
     * 测试更新用户身份
     * @throws Exception
     */
    @Test
    public void updateIdentity()throws Exception{
        int statusCode = identityService.updateIdentity("new31-user","Org1");
        assertEquals(statusCode, HttpStatus.OK.value());
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
