package com.hx.huixing.model;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class AwemeVO implements Serializable {
    private Long aweme_id;
    private String desc;
    private Integer aweme_type;
    private VideoVO video;
}
