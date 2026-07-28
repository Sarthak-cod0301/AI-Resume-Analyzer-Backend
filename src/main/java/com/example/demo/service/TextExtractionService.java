// service/TextExtractionService.java
package com.example.demo.service;

import com.example.demo.exception.AnalysisException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class TextExtractionService {

    public String extractText(String filePath, String fileType) {
        try {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> extractFromPdf(filePath);
                case "docx" -> extractFromDocx(filePath);
                default -> throw new AnalysisException("Unsupported file type: " + fileType);
            };
      } catch (IOException e) {
    e.printStackTrace();
    throw new AnalysisException(
            "Failed to extract text from resume file: " + e.getMessage(), e);
}
    }

private String extractFromPdf(String filePath) throws IOException {

    File file = new File(filePath);

    System.out.println("======================================");
    System.out.println("FILE PATH = " + file.getAbsolutePath());
    System.out.println("EXISTS = " + file.exists());
    System.out.println("CAN READ = " + file.canRead());
    System.out.println("FILE SIZE = " + (file.exists() ? file.length() : "NOT FOUND"));
    System.out.println("======================================");

    try (PDDocument document = Loader.loadPDF(file)) {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    }
}

    private String extractFromDocx(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
