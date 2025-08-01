package com.example.blog.config;

import com.example.blog.util.SnowflakeIdGenerator;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Hibernate雪花算法ID生成策略
 */
@Component
public class SnowflakeIdGeneratorStrategy implements IdentifierGenerator {

    private static SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    public void setSnowflakeIdGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        SnowflakeIdGeneratorStrategy.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        // 如果ID已经存在，就使用已存在的ID
        Object idObj = session.getEntityPersister(null, object).getIdentifier(object, session);
        if (idObj != null && idObj instanceof Serializable) {
            return (Serializable) idObj;
        }
        
        // 否则生成新的雪花ID
        if (snowflakeIdGenerator == null) {
            snowflakeIdGenerator = new SnowflakeIdGenerator();
        }
        return snowflakeIdGenerator.nextId();
    }
} 