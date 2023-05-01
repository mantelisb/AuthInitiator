package com.company.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessages {
    private List<FieldError> fields;
}
