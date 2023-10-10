package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.enums.ResultCode;
//import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
//import com.siheng.metadataplatform.pojo.TblRelationShipList;
import com.siheng.metadataplatform.pojo.TblRelationShip;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.service.TblService;
import com.siheng.metadataplatform.service.UserService;
import com.siheng.metadataplatform.service.impl.TblServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class HelloController {

    @Autowired
    private UserService userService;


    @Autowired
    private SqlLineageService sqlLineageService;

//    @Autowired
//    private TblMapper tblMapper;

    @Autowired
    private TblService tblService;


    @PostMapping(value = "/test_webhook")
    public ResultDto sayHello(@RequestBody Map<Object, Object> map) {

        try {
            if (map.containsKey("commits")) {
                int i = sqlLineageService.updateSqlLineage(map);
                if (i == 0) return ResultDto.success();
            } else {
                return ResultDto.success(ResultCode.SUCCESS_NOT_DATA.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }

        return null;
//        return userService.queryUser();
    }


    @GetMapping(value = "/testDb")
    public String testDb(@RequestBody Map<String, Object> map) {
//        System.out.println(map);
//        String s = userService.queryUser();

        return null;
    }


    @GetMapping(value = "/testNeo4j")
    public String testNeo4j() {


//        int i = tblMapper.insertTblRelationShip(tbl);
//        System.out.println("insert的结果是   " + i);

//        List<String> list = new ArrayList();
//        list.add("ods_a");
//        list.add("ods_b");
//        list.add("ods_c");
//        list.add("ods_d");
//        list.add("ods_e");
//        list.add("ods_f");
//        list.add("ods_g");
//        int i = tblMapper.insertTblList(list);
//
//        TblRelationShipList tblRelationShipList = new TblRelationShipList();
//        tblRelationShipList.setAllTblList(Arrays.asList("ods_a","ods_b","ods_c"));
//        tblRelationShipList.setSourceTblList(Arrays.asList("ods_a","ods_b"));
//        tblRelationShipList.setTargetTbl("ods_c");
//        int i = tblMapper.insertTblRelationShipList(tblRelationShipList);
//        System.out.println("insert" + i);


        Set<String> set = new HashSet<>();
        tblService.insertTblList(set);



        return null;
    }



    @GetMapping(value = "/testNeo4j/insertTblList")
    public String insertTblList() {


        Set<String> set = new HashSet<>();
        set.add("ods_a");
        set.add("ods_b");
        set.add("ods_c");
        set.add("ods_d");
        set.add("ods_d");
        set.add("ods_e");
        set.add("ods_f");
        set.add("ods_f");
        set.add("ods_g");
        set.add("ods_h");
        set.add("ods_i");
        tblService.insertTblList(set);



        return null;
    }




    @GetMapping(value = "/testNeo4j/insertTblRelationShipList")
    public String insertTblRelationShipList() {

        Set<String> allSet = new HashSet<>();
        allSet.add("ods_a");
        allSet.add("ods_b");
        allSet.add("ods_c");
        allSet.add("ods_i");


        Set<String> sourceSet = new HashSet<>();
        sourceSet.add("ods_a");
        sourceSet.add("ods_b");
        sourceSet.add("ods_c");
        sourceSet.add("ods_i");

        String targetTbl = "ods_i";

        sourceSet.remove(targetTbl);

        TblRelationShip tblRelationShip = new TblRelationShip();
        tblRelationShip.setAllTbls(allSet);
        tblRelationShip.setSourceTbls(sourceSet);
        tblRelationShip.setTargetTbl(targetTbl);

        tblService.insertTblRelationShipList(tblRelationShip);




        return null;
    }

    @GetMapping(value = "/testNeo4j/deleteTblAndAllTblRelationShipList")
    public String deleteTblAndAllTblRelationShipList() {

        String deleteTabName = "ods_c";
        tblService.deleteTblAndAllTblRelationShipList(deleteTabName);

        return null;
    }


}