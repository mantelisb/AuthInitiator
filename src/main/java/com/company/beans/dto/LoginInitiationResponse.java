package com.company.beans.dto;

import com.company.beans.Links;
import com.company.beans.LoginStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginInitiationResponse {
    private LoginStatus status;
    private Links links;

}
