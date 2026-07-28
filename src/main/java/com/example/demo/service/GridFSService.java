package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class GridFSService {

    private final GridFsTemplate gridFsTemplate;

    public String saveFile(InputStream inputStream,
                           String filename,
                           String contentType) {

        ObjectId id = gridFsTemplate.store(
                inputStream,
                filename,
                contentType
        );

        return id.toHexString();
    }

    public GridFsResource getFile(String fileId) {

        return gridFsTemplate.getResource(
                gridFsTemplate.findOne(
                        new org.springframework.data.mongodb.core.query.Query(
                                org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                        .is(new ObjectId(fileId))
                        )
                )
        );
    }

    public void deleteFile(String fileId) {

        gridFsTemplate.delete(
                new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                .is(new ObjectId(fileId))
                )
        );
    }
}
