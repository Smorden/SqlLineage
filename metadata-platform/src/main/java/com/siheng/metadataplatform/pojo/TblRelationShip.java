package com.siheng.metadataplatform.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * @author Dearest
 * @date 2023/10/9 3:01 下午
 * @Desc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TblRelationShip {

    private Set<String> allTbls;

    private Set<String> sourceTbls;

    private String targetTbl;

}
