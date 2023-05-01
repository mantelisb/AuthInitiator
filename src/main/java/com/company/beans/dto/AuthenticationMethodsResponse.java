package com.company.beans.dto;

import com.company.beans.AuthenticationMethod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;


@EqualsAndHashCode
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationMethodsResponse {
    private List<AuthenticationMethod> authenticationMethods;

}