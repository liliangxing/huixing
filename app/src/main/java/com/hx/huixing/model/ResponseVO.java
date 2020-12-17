package com.hx.huixing.model;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ResponseVO implements Serializable {
    private Integer status_code;
    private List<AwemeVO> aweme_list;
}
