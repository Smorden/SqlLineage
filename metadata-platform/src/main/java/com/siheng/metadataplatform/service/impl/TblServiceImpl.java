package com.siheng.metadataplatform.service.impl;

import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
import com.siheng.metadataplatform.pojo.TblRelationShip;
import com.siheng.metadataplatform.service.TblService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author Dearest
 * @date 2023/10/10 2:20 下午
 * @Desc
 */
@Service
public class TblServiceImpl implements TblService {

    @Autowired
    private TblMapper tblMapper;


    @Override
    public void insertTblList(Set<String> set) {
        tblMapper.insertTblList(set);
    }

    @Override
    public void insertTblRelationShipList(TblRelationShip tblRelationShip) {
        tblMapper.insertTblRelationShipList(tblRelationShip);
    }

    @Override
    public void deleteTblAndAllTblRelationShipList(String deleteTabName) {
        tblMapper.deleteTblAndAllTblRelationShipList(deleteTabName);
    }
}
