package com.company;

import com.company.beans.AuthenticationMethod;
import com.company.beans.Link;
import com.company.beans.LoginStatus;
import com.company.beans.dto.LoginInitiationResponse;

import java.util.List;

public interface LoginAdapter {

    List<AuthenticationMethod> getAllLoginMethods() throws Exception;

    LoginInitiationResponse initiateLogin(String userId, AuthenticationMethod method) throws Exception;

    LoginStatus getLoginStatus(Link link) throws Exception;

}
