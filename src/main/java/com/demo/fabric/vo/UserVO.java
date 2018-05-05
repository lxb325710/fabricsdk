package com.demo.fabric.vo;

import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.Attribute;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class UserVO implements User, Serializable {
    private static final long serialVersionUID = 8077132186383604355L;

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    @Getter
    private String organization;
    private String enrollmentSecret;
    Enrollment enrollment = null; //need access in test env.

    @Getter
    @Setter
    private int maxEnrollments = -1;

    @Getter
    @Setter
    List<Attribute> attributes=null;

    public UserVO(String name, String org) {
        this.name = name;
        this.organization = org;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<String> roles) {

        this.roles = roles;
    }

    @Override
    public String getAccount() {
        return this.account;
    }

    /**
     * Set the account.
     *
     * @param account The account.
     */
    public void setAccount(String account) {

        this.account = account;
    }

    @Override
    public String getAffiliation() {
        return this.affiliation;
    }

    /**
     * Set the affiliation.
     *
     * @param affiliation the affiliation.
     */
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public Enrollment getEnrollment() {
        return this.enrollment;
    }

    /**
     * Determine if this name has been registered.
     *
     * @return {@code true} if registered; otherwise {@code false}.
     */
    public boolean isRegistered() {
        return !StringUtil.isNullOrEmpty(enrollmentSecret);
    }

    /**
     * Determine if this name has been enrolled.
     *
     * @return {@code true} if enrolled; otherwise {@code false}.
     */
    public boolean isEnrolled() {
        return this.enrollment != null;
    }

    public String getEnrollmentSecret() {
        return enrollmentSecret;
    }

    public void setEnrollmentSecret(String enrollmentSecret) {
        this.enrollmentSecret = enrollmentSecret;
    }

    public void setEnrollment(Enrollment enrollment) {

        this.enrollment = enrollment;
    }

    public static String toKeyValStoreName(String name, String org) {
        return "user." + name + org;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

    String mspId;

    public void setMspId(String mspID) {
        this.mspId = mspID;
    }

}
