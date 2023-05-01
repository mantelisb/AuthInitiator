package com.company.beans.dto;

import com.company.beans.ErrorMessages;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpException {
    private ErrorMessages errorMessages;
}
