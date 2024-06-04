package com.siheng.metadataplatform.mapper.neo4j;

import com.siheng.metadataplatform.pojo.TblRelationShip;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Dearest
 * @date 2023/10/9 9:35 上午
 * @Desc
 */
public interface TblMapper {


    void insertTblList(@Param("tblSet") Set<String> tblSet);


    void insertTblRelationShipList(TblRelationShip tblRelationShip);

    void deleteTblAndAllTblRelationShipList(@Param("tblName") String tblName);

    void deleteAllTblAndAllTblRelationShip();

    void deleteBeforeAllTblRelationShipList(@Param("tblName") String tblName);


}
