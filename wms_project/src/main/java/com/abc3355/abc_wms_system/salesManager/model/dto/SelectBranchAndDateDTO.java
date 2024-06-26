package com.abc3355.abc_wms_system.salesManager.model.dto;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class SelectBranchAndDateDTO {

    private int no;
    private String startDate;
    private String endDate;
}
