package com.github.eiriksgata.rulateday.pojo;


import lombok.Data;

@Data
public class CardsGroupData {
    private Long id;
    private Long groupId;
    private Long typeId;
    private String value;
    private Boolean autoShuffle;
}
