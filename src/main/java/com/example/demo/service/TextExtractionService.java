package com.example.demo.service;

import com.example.demo.exception.AnalysisException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class TextExtractionService {

    private final GridFSService gridFSService;

   public String extractText(String gridFsId, String fileType) {

    System.out.println("===== TEXT EXTRACTION =====");
    System.out.println("GridFS ID: " + gridFsId);
    System.out.println("File Type: " + fileType);

    try {
        GridFsResource resource = gridFSService.getFile(gridFsId);

        System.out.println("Resource = " + resource);

        if (resource == null) {
            System.out.println("Resource is NULL");
            throw new RuntimeException("Resume not found");
        }

        System.out.println("Exists = " + resource.exists());
        System.out.println("Filename = " + resource.getFilename());

        try (InputStream inputStream = resource.getInputStream()) {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> extractPdf(inputStream);
                case "docx" -> extractDocx(inputStream);
                default -> throw new RuntimeException("Unsupported file");
            };
        }

    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Extraction failed", e);
    }
}

    private String extractPdf(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
