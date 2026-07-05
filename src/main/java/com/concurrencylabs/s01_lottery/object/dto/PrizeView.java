package com.concurrencylabs.s01_lottery.object.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 對外呈現的中獎獎品資訊（邊界 DTO）。 */
@Data
@AllArgsConstructor
public class PrizeView {

    private Long id;

    private String name;

    private Integer level;
}
