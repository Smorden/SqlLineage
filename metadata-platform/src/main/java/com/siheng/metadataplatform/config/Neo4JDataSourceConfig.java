package com.siheng.metadataplatform.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * @author Dearest
 * @date 2023/9/6 4:21 下午
 * @Desc
 */
@Configuration
@MapperScan(basePackages = {"com.siheng.metadataplatform.mapper.neo4j"}, sqlSessionFactoryRef = "neo4jSqlSessionFactory")
public class Neo4JDataSourceConfig {

    @Bean(name = "neo4jDataSource")
    @ConfigurationProperties("spring.datasource.neo4j")
    public DataSource dataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(name = "neo4jTransactionManager")
    public DataSourceTransactionManager dataSourceTransactionManager(@Qualifier("neo4jDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "neo4jSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("neo4jDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/neo4j/*.xml"));
        return factoryBean.getObject();
    }

    @Bean(name = "neo4jSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("neo4jSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}
