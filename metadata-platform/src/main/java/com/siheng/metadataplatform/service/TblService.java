package com.siheng.metadataplatform.service;

import com.siheng.metadataplatform.pojo.TblRelationShip;

import java.util.Set;

/**
 * @author Dearest
 * @date 2023/10/10 2:20 下午
 * @Desc
 */
public interface TblService {

    void insertTblList(Set<String> set);

    void insertTblRelationShipList(TblRelationShip tblRelationShip);

    void deleteTblAndAllTblRelationShipList(String deleteTabName);
}
