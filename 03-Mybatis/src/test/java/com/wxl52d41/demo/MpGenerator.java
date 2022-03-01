package com.wxl52d41.demo;

import com.baomidou.mybatisplus.annotation.DbType;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.converts.OracleTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author yfzhao9
 * @Date 2021/8/12 20:02
 * @Version V1.0
 **/

public class MpGenerator {

    public static void main(String[] args) {
        AutoGenerator mpg = new AutoGenerator();
        GlobalConfig gc = new GlobalConfig();
        //生成代码在本地的路径
        gc.setOutputDir("D:\\04工作资料\\01工作相关\\预算资料");
        //作者
        gc.setAuthor("xlwang55");
        gc.setFileOverride(true);
        gc.setActiveRecord(true);
        gc.setEnableCache(false);
        gc.setBaseResultMap(true);
        gc.setBaseColumnList(true);
        mpg.setGlobalConfig(gc);
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setDbType(DbType.ORACLE);
        dsc.setTypeConvert(new MySqlTypeConvert());
        dsc.setDriverName("com.mysql.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("wxlalt");
        dsc.setUrl("jdbc:mysql://139.9.141.175:3306/mybatisTest?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true");
        mpg.setDataSource(dsc);
        PackageConfig pc = new PackageConfig();
        pc.setParent("com.wxl52d41.demo");
        //模块名
//        pc.setModuleName("flow");
//        pc.setController("controller");
        pc.setEntity("entity");
        pc.setMapper("mapper");
        pc.setService("service");
        pc.setServiceImpl("service.impl");
        pc.setXml("mapper.xml");
        mpg.setPackageInfo(pc);
        StrategyConfig strategy = new StrategyConfig();
        //需要生成的表，注意一定要大写！大写！否则识别不出来！可写多个：new String[]{"EXCEL", "USER"}
        strategy.setInclude(new String[]{"user"});
//        strategy.setTablePrefix("T_");
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityLombokModel(true);
        mpg.setStrategy(strategy);
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("abc", this.getConfig().getGlobalConfig().getAuthor() + "-mp");
                this.setMap(map);
            }
        };
        mpg.setCfg(cfg);
        mpg.execute();
    }
}
