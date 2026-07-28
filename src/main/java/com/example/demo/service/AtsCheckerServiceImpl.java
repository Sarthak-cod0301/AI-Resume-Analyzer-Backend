// service/AtsCheckerServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.AtsCheckResponseDTO;
import com.example.demo.dto.AtsIssueDTO;
import com.example.demo.entity.AtsCheckResult;
import com.example.demo.entity.Resume;
import com.example.demo.exception.AtsCheckException;
import com.example.demo.repository.AtsCheckResultRepository;
import com.example.demo.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtsCheckerServiceImpl implements AtsCheckerService {

    private final ResumeRepository resumeRepository;
    private final AtsCheckResultRepository atsCheckResultRepository;

    private static final List<String> STANDARD_HEADINGS = List.of(
            "experience", "work experience", "education", "skills",
            "projects", "summary", "objective", "certifications", "achievements"
    );

    private static final Set<String> RISKY_FONT_KEYWORDS = Set.of(
            "comic sans", "papyrus", "curlz", "brush script", "jokerman"
    );

    private static final long MAX_RECOMMENDED_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MIN_WORD_COUNT = 300;
    private static final int MAX_WORD_COUNT = 1000;
    private static final int MAX_RECOMMENDED_PAGES = 2;

    @Override
    public AtsCheckResponseDTO runCheck(String resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new AtsCheckException("Resume not found or not owned by user"));

        List<AtsIssueDTO> issues = new ArrayList<>();
        int score = 100;

        long fileSize;
        try {
            fileSize = Files.size(Paths.get(resume.getGridFsId()));
        } catch (Exception e) {
            throw new AtsCheckException("Could not read resume file from disk", e);
        }

        AnalysisContext ctx;
        if ("pdf".equalsIgnoreCase(resume.getFileType())) {
            ctx = analyzePdf(resume.getGridFsId());
        } else if ("docx".equalsIgnoreCase(resume.getFileType())) {
            ctx = analyzeDocx(resume.getGridFsId());
        } else {
            throw new AtsCheckException("Unsupported file type for ATS check: " + resume.getFileType());
        }

        if (ctx.riskyFontsFound) {
            issues.add(issue("Font", "HIGH",
                    "Decorative/non-standard fonts detected",
                    "Use standard fonts (Arial, Calibri, Times New Roman)"));
            score -= 10;
        }

        if (ctx.tableCount > 0) {
            issues.add(issue("Tables", "HIGH",
                    "Resume contains " + ctx.tableCount + " table(s) - ATS may misread column order",
                    "Avoid tables"));
            score -= 15;
        }

        if (ctx.imageCount > 0) {
            issues.add(issue("Images", "MEDIUM",
                    "Resume contains " + ctx.imageCount + " embedded image(s)",
                    "Remove graphics"));
            score -= 10;
        }

        if (ctx.iconLikeImageCount > 0) {
            issues.add(issue("Icons", "LOW",
                    "Possible icon graphics detected (e.g. phone/email symbols)",
                    "Replace icons with plain text labels"));
            score -= 5;
        }

        if (ctx.hasHeader) {
            issues.add(issue("Headers", "MEDIUM",
                    "Content found in document header - many ATS systems skip headers entirely",
                    "Move contact info out of the header into the main body"));
            score -= 10;
        }

        if (ctx.hasFooter) {
            issues.add(issue("Footer", "MEDIUM",
                    "Content found in document footer - may not be parsed by ATS",
                    "Move content out of the footer into the main body"));
            score -= 10;
        }

        if (!ctx.hasStandardHeadings) {
            issues.add(issue("Headings", "MEDIUM",
                    "No standard section headings found (Experience, Education, Skills, etc.)",
                    "Use standard headings"));
            score -= 10;
        }

        double density = calculateKeywordDensity(ctx.fullText);
        if (density < 1.5) {
            issues.add(issue("Keyword Density", "MEDIUM",
                    String.format("Keyword density is low (%.1f%%)", density),
                    "Increase keywords"));
            score -= 10;
        } else if (density > 6.0) {
            issues.add(issue("Keyword Density", "LOW",
                    String.format("Keyword density is unusually high (%.1f%%) - may look like stuffing", density),
                    "Reduce repeated keywords, vary phrasing"));
            score -= 5;
        }

        if (fileSize > MAX_RECOMMENDED_FILE_SIZE) {
            issues.add(issue("File Size", "LOW",
                    String.format("File size is %.2f MB - large files can slow down ATS uploads", fileSize / 1024.0 / 1024.0),
                    "Compress file to under 2MB"));
            score -= 5;
        }

        int wordCount = ctx.fullText.trim().isEmpty() ? 0 : ctx.fullText.trim().split("\\s+").length;

        if (wordCount < MIN_WORD_COUNT) {
            issues.add(issue("Resume Length", "MEDIUM",
                    "Resume is too short (" + wordCount + " words) - may lack sufficient detail",
                    "Expand experience and project descriptions"));
            score -= 10;
        } else if (wordCount > MAX_WORD_COUNT) {
            issues.add(issue("Resume Length", "MEDIUM",
                    "Resume is too long (" + wordCount + " words) - ATS/recruiters prefer concise resumes",
                    "Trim to 1-2 pages, remove redundant details"));
            score -= 10;
        }

        if (ctx.pageCount != null && ctx.pageCount > MAX_RECOMMENDED_PAGES) {
            issues.add(issue("Resume Length", "LOW",
                    "Resume spans " + ctx.pageCount + " pages",
                    "Keep resume to 1-2 pages"));
            score -= 5;
        }

        score = Math.max(0, Math.min(100, score));

        List<String> suggestions = issues.stream()
                .map(AtsIssueDTO::getSuggestion)
                .distinct()
                .collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            suggestions.add("No major ATS issues detected - resume looks well-structured");
        }

        AtsCheckResult entity = AtsCheckResult.builder()
                .resumeId(resumeId)
                .userId(userId)
                .atsScore(score)
                .suggestions(suggestions)
                .issues(issues)
                .wordCount(wordCount)
                .pageCount(ctx.pageCount)
                .fileSizeBytes(fileSize)
                .keywordDensityPercent(density)
                .checkedAt(LocalDateTime.now())
                .build();

        entity = atsCheckResultRepository.save(entity);
        return toDTO(entity);
    }

    private AnalysisContext analyzePdf(String filePath) {
        AnalysisContext ctx = new AnalysisContext();
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            ctx.pageCount = document.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            ctx.fullText = stripper.getText(document);

            Set<String> fontNames = new HashSet<>();
            for (PDPage page : document.getPages()) {
                if (page.getResources() != null) {
                    for (org.apache.pdfbox.cos.COSName fontName : page.getResources().getFontNames()) {
                        PDFont font = page.getResources().getFont(fontName);
                        if (font != null && font.getName() != null) {
                            fontNames.add(font.getName().toLowerCase());
                        }
                    }
                    for (org.apache.pdfbox.cos.COSName xObjName : page.getResources().getXObjectNames()) {
                        var xObj = page.getResources().getXObject(xObjName);
                        if (xObj instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject img) {
                            ctx.imageCount++;
                            if (img.getWidth() < 60 && img.getHeight() < 60) {
                                ctx.iconLikeImageCount++;
                            }
                        }
                    }
                }
            }
            ctx.riskyFontsFound = fontNames.stream()
                    .anyMatch(f -> RISKY_FONT_KEYWORDS.stream().anyMatch(f::contains));

            ctx.tableCount = detectTableLikePatterns(ctx.fullText);
            ctx.hasStandardHeadings = containsStandardHeading(ctx.fullText);

            // PDF header/footer region detection needs coordinate-level layout analysis
            // (PDFTextStripperByArea) — conservatively skipped here rather than guessed.
            ctx.hasHeader = false;
            ctx.hasFooter = false;

        } catch (Exception e) {
            throw new AtsCheckException("Failed to analyze PDF for ATS check", e);
        }
        return ctx;
    }

    private AnalysisContext analyzeDocx(String filePath) {
        AnalysisContext ctx = new AnalysisContext();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder textBuilder = new StringBuilder();
            Set<String> fontNames = new HashSet<>();

            for (XWPFParagraph para : document.getParagraphs()) {
                textBuilder.append(para.getText()).append("\n");
                for (XWPFRun run : para.getRuns()) {
                    if (run.getFontFamily() != null) {
                        fontNames.add(run.getFontFamily().toLowerCase());
                    }
                }
            }

            ctx.tableCount = document.getTables().size();
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        textBuilder.append(cell.getText()).append(" ");
                    }
                }
            }

            ctx.imageCount = document.getAllPictures().size();
            for (var picture : document.getAllPictures()) {
                if (picture.getData() != null && picture.getData().length < 15_000) {
                    ctx.iconLikeImageCount++;
                }
            }

            ctx.hasHeader = document.getHeaderList() != null &&
                    document.getHeaderList().stream()
                            .anyMatch(h -> h.getText() != null && !h.getText().isBlank());

            ctx.hasFooter = document.getFooterList() != null &&
                    document.getFooterList().stream()
                            .anyMatch(f -> f.getText() != null && !f.getText().isBlank());

            ctx.fullText = textBuilder.toString();
            ctx.riskyFontsFound = fontNames.stream()
                    .anyMatch(f -> RISKY_FONT_KEYWORDS.stream().anyMatch(f::contains));
            ctx.hasStandardHeadings = containsStandardHeading(ctx.fullText);
            ctx.pageCount = null;

        } catch (Exception e) {
            throw new AtsCheckException("Failed to analyze DOCX for ATS check", e);
        }
        return ctx;
    }

    private boolean containsStandardHeading(String text) {
        String lower = text.toLowerCase();
        return STANDARD_HEADINGS.stream().anyMatch(lower::contains);
    }

    private int detectTableLikePatterns(String pdfText) {
        long linesWithMultipleTabs = pdfText.lines()
                .filter(line -> line.chars().filter(c -> c == '\t').count() >= 2)
                .count();
        return linesWithMultipleTabs > 3 ? 1 : 0;
    }

    private double calculateKeywordDensity(String text) {
        if (text == null || text.isBlank()) return 0.0;
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        if (words.length == 0) return 0.0;

        Map<String, Long> freq = Arrays.stream(words)
                .filter(w -> w.length() > 3)
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        long repeatedWordOccurrences = freq.values().stream()
                .filter(count -> count > 1)
                .mapToLong(Long::longValue)
                .sum();

        return (repeatedWordOccurrences * 100.0) / words.length;
    }

    private AtsIssueDTO issue(String category, String severity, String message, String suggestion) {
        return AtsIssueDTO.builder()
                .category(category)
                .severity(severity)
                .message(message)
                .suggestion(suggestion)
                .build();
    }

    @Override
    public List<AtsCheckResponseDTO> getHistory(String userId) {
        return atsCheckResultRepository.findByUserIdOrderByCheckedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AtsCheckResponseDTO getById(String id, String userId) {
        AtsCheckResult entity = atsCheckResultRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AtsCheckException("ATS check result not found"));
        return toDTO(entity);
    }

    private AtsCheckResponseDTO toDTO(AtsCheckResult entity) {
        return AtsCheckResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .atsScore(entity.getAtsScore())
                .suggestions(entity.getSuggestions())
                .issues(entity.getIssues())
                .wordCount(entity.getWordCount())
                .pageCount(entity.getPageCount())
                .fileSizeBytes(entity.getFileSizeBytes())
                .keywordDensityPercent(entity.getKeywordDensityPercent())
                .checkedAt(entity.getCheckedAt())
                .build();
    }

    private static class AnalysisContext {
        String fullText = "";
        int tableCount = 0;
        int imageCount = 0;
        int iconLikeImageCount = 0;
        boolean hasHeader = false;
        boolean hasFooter = false;
        boolean riskyFontsFound = false;
        boolean hasStandardHeadings = false;
        Integer pageCount = null;
    }
}
