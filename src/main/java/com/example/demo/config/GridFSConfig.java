package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
public class GridFSConfig {

    @Bean
    public GridFsTemplate gridFsTemplate(
            org.springframework.data.mongodb.MongoDatabaseFactory factory,
            org.springframework.data.mongodb.core.convert.MongoConverter converter) {

        return new GridFsTemplate(factory, converter);
    }
}
