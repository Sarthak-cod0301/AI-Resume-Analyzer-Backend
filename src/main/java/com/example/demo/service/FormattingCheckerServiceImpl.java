// service/FormattingCheckerServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.FormattingCheckResponseDTO;
import com.example.demo.dto.FormattingIssueDTO;
import com.example.demo.entity.FormattingCheckResult;
import com.example.demo.entity.Resume;
import com.example.demo.exception.FormattingCheckException;
import com.example.demo.repository.FormattingCheckResultRepository;
import com.example.demo.repository.ResumeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormattingCheckerServiceImpl implements FormattingCheckerService {

    private final ResumeRepository resumeRepository;
    private final FormattingCheckResultRepository formattingCheckResultRepository;
    private final TextExtractionService textExtractionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final Set<String> BULLET_CHAR_CATEGORIES = Set.of("•", "-", "*", "▪", "‣", "◦");

    @Override
    public FormattingCheckResponseDTO runCheck(String resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new FormattingCheckException("Resume not found or not owned by user"));

        List<FormattingIssueDTO> issues = new ArrayList<>();
        int ruleScore = 100;
        AnalysisContext ctx;

        if ("docx".equalsIgnoreCase(resume.getFileType())) {
            ctx = analyzeDocx(resume.getResumePath());
        } else if ("pdf".equalsIgnoreCase(resume.getFileType())) {
            ctx = analyzePdf(resume.getResumePath());
        } else {
            throw new FormattingCheckException("Unsupported file type: " + resume.getFileType());
        }

        // ---- Rule-based checks ----
        if (ctx.distinctFonts.size() > 2) {
            issues.add(issue("Font Consistency", "HIGH",
                    "Resume uses " + ctx.distinctFonts.size() + " different fonts: " + String.join(", ", ctx.distinctFonts),
                    "Use a maximum of 1-2 fonts throughout (e.g. one for headings, one for body)", "RULE_BASED"));
            ruleScore -= 15;
        }

        if (ctx.distinctFontSizes.size() > 4) {
            issues.add(issue("Font Consistency", "MEDIUM",
                    "Resume uses " + ctx.distinctFontSizes.size() + " different font sizes",
                    "Limit to 2-3 font sizes (e.g. name, section headings, body text)", "RULE_BASED"));
            ruleScore -= 10;
        }

        if (ctx.bulletStyles.size() > 1) {
            issues.add(issue("Bullet Points", "MEDIUM",
                    "Multiple bullet styles detected: " + String.join(" ", ctx.bulletStyles),
                    "Use a single consistent bullet style throughout", "RULE_BASED"));
            ruleScore -= 10;
        }

        if (ctx.paragraphSpacingVariants.size() > 3) {
            issues.add(issue("Spacing", "MEDIUM",
                    "Inconsistent spacing detected between paragraphs (" + ctx.paragraphSpacingVariants.size() + " different spacing values)",
                    "Use consistent spacing before/after paragraphs and sections", "RULE_BASED"));
            ruleScore -= 10;
        }
        if (ctx.hasExcessiveBlankLines) {
            issues.add(issue("Spacing", "LOW",
                    "Multiple consecutive blank lines detected",
                    "Remove extra blank lines; use consistent single spacing between sections", "RULE_BASED"));
            ruleScore -= 5;
        }

        if (ctx.alignmentVariants.size() > 2) {
            issues.add(issue("Alignment", "MEDIUM",
                    "Text alignment varies across the document (" + ctx.alignmentVariants.size() + " different alignments used)",
                    "Keep body text left-aligned; use center alignment only for the name/header if desired", "RULE_BASED"));
            ruleScore -= 10;
        }

        if (!ctx.headingsConsistentlyFormatted) {
            issues.add(issue("Headings", "HIGH",
                    "Section headings are not formatted consistently (varying bold/size/case)",
                    "Format all section headings the same way (same font size, bold, and case)", "RULE_BASED"));
            ruleScore -= 15;
        }
        if (ctx.headingCount == 0) {
            issues.add(issue("Headings", "MEDIUM",
                    "No clearly distinguishable section headings detected",
                    "Use bold, slightly larger text for section headings (Experience, Education, Skills, etc.)", "RULE_BASED"));
            ruleScore -= 10;
        }

        ruleScore = Math.max(0, Math.min(100, ruleScore));

        // ---- AI layer: qualitative pass on top, especially valuable for PDF ----
        AiFormattingRaw aiResult = runAiFormattingReview(ctx.fullText, resume.getFileType());
        for (AiFormattingIssueRaw aiIssue : aiResult.issues) {
            issues.add(issue(aiIssue.category, aiIssue.severity, aiIssue.message, aiIssue.suggestion, "AI"));
        }

        // Blend: rule-based is weighted higher since it's objective; AI fills gaps/nuance
        int aiScore = Math.max(0, Math.min(100, aiResult.formattingScore));
        int blendedScore = (int) Math.round((ruleScore * 0.6) + (aiScore * 0.4));

        List<String> suggestions = issues.stream()
                .map(FormattingIssueDTO::getSuggestion)
                .distinct()
                .collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            suggestions.add("Formatting looks consistent - no major issues detected");
        }

        FormattingCheckResult entity = FormattingCheckResult.builder()
                .resumeId(resumeId)
                .userId(userId)
                .formattingScore(blendedScore)
                .ruleBasedScore(ruleScore)
                .aiAssessedScore(aiScore)
                .aiOverallImpression(aiResult.overallImpression)
                .issues(issues)
                .suggestions(suggestions)
                .distinctFontsUsed(ctx.distinctFonts.size())
                .distinctFontSizesUsed(ctx.distinctFontSizes.size())
                .bulletStyleVariantsUsed(ctx.bulletStyles.size())
                .checkedAt(LocalDateTime.now())
                .build();

        entity = formattingCheckResultRepository.save(entity);
        return toDTO(entity);
    }

    // ---------- AI formatting review ----------
    private AiFormattingRaw runAiFormattingReview(String resumeText, String fileType) {
        String prompt = """
            You are a professional resume formatting reviewer. You are given the RAW TEXT extracted
            from a %s resume (line breaks and spacing in this text roughly reflect the original
            layout, though exact visual alignment/fonts are not directly visible to you).

            Based on the TEXT PATTERNS you can observe - heading casing/style consistency, bullet
            point consistency as reflected in the text, spacing between sections, line length
            consistency, and overall visual organization implied by the text - give a qualitative
            formatting review covering these categories: Alignment, Font Consistency, Bullet Points,
            Spacing, Headings.

            Return ONLY a valid JSON object (no markdown, no extra text) with EXACTLY this structure:

            {
              "formattingScore": <integer 0-100, your overall formatting quality assessment>,
              "overallImpression": "<1-2 sentence overall impression of how well-formatted this resume appears to be>",
              "issues": [
                {
                  "category": "<Alignment | Font Consistency | Bullet Points | Spacing | Headings>",
                  "severity": "<HIGH | MEDIUM | LOW>",
                  "message": "<what you observed>",
                  "suggestion": "<specific actionable fix>"
                }
              ]
            }

            Rules:
            - Only flag things you can reasonably infer from text patterns (e.g. inconsistent bullet
              characters, section headings that aren't clearly distinguished, irregular spacing
              patterns, inconsistent capitalization of headings).
            - Do not comment on things impossible to know from plain text (exact font names, colors,
              pixel-level alignment) - stick to structural/textual patterns only.
            - Limit to the 5 most useful observations.
            - Return an empty issues array if formatting looks clean.
            - Return ONLY the JSON object, nothing else.

            RESUME TEXT (%s):
            %s
            """.formatted(fileType.toUpperCase(), fileType.toUpperCase(), resumeText);

        try {
            String response = geminiService.generateContent(prompt);
            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, AiFormattingRaw.class);
        } catch (Exception e) {
            // If the AI pass fails, don't fail the whole check - fall back to rule-based only
            AiFormattingRaw fallback = new AiFormattingRaw();
            fallback.formattingScore = 70; // neutral default, doesn't unfairly penalize
            fallback.overallImpression = "AI formatting review unavailable for this check.";
            fallback.issues = new ArrayList<>();
            return fallback;
        }
    }

    // ---------- DOCX analysis (rich structural access) ----------
    private AnalysisContext analyzeDocx(String filePath) {
        AnalysisContext ctx = new AnalysisContext();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            int blankLineStreak = 0;
            StringBuilder textBuilder = new StringBuilder();

            for (XWPFParagraph para : paragraphs) {
                String text = para.getText();
                textBuilder.append(text == null ? "" : text).append("\n");

                if (text == null || text.isBlank()) {
                    blankLineStreak++;
                    if (blankLineStreak >= 3) ctx.hasExcessiveBlankLines = true;
                    continue;
                } else {
                    blankLineStreak = 0;
                }

                if (para.getAlignment() != null) {
                    ctx.alignmentVariants.add(para.getAlignment().toString());
                }

                int spacingKey = (para.getSpacingBefore() >= 0 ? para.getSpacingBefore() : 0)
                        + (para.getSpacingAfter() >= 0 ? para.getSpacingAfter() : 0);
                ctx.paragraphSpacingVariants.add(spacingKey);

                String trimmed = text.trim();
                if (!trimmed.isEmpty() && BULLET_CHAR_CATEGORIES.contains(String.valueOf(trimmed.charAt(0)))) {
                    ctx.bulletStyles.add(String.valueOf(trimmed.charAt(0)));
                } else if (para.getNumID() != null) {
                    ctx.bulletStyles.add("numbered-list-style");
                }

                boolean looksLikeHeading = text.length() < 40 && isBoldRun(para);
                if (looksLikeHeading) {
                    ctx.headingCount++;
                    ctx.headingFontSizes.add(getMaxFontSize(para));
                }

                for (XWPFRun run : para.getRuns()) {
                    if (run.getFontFamily() != null) {
                        ctx.distinctFonts.add(run.getFontFamily());
                    }
                    if (run.getFontSize() > 0) {
                        ctx.distinctFontSizes.add(run.getFontSize());
                    }
                }
            }

            ctx.headingsConsistentlyFormatted = ctx.headingFontSizes.size() <= 1;
            ctx.fullText = textBuilder.toString();

        } catch (Exception e) {
            throw new FormattingCheckException("Failed to analyze DOCX formatting", e);
        }
        return ctx;
    }

    private boolean isBoldRun(XWPFParagraph para) {
        return para.getRuns().stream().anyMatch(XWPFRun::isBold);
    }

    private int getMaxFontSize(XWPFParagraph para) {
        return para.getRuns().stream()
                .mapToInt(XWPFRun::getFontSize)
                .filter(size -> size > 0)
                .max()
                .orElse(0);
    }

    // ---------- PDF analysis (limited layout signal - AI layer compensates) ----------
    private AnalysisContext analyzePdf(String filePath) {
        AnalysisContext ctx = new AnalysisContext();
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {

            for (PDPage page : document.getPages()) {
                if (page.getResources() != null) {
                    for (org.apache.pdfbox.cos.COSName fontName : page.getResources().getFontNames()) {
                        PDFont font = page.getResources().getFont(fontName);
                        if (font != null && font.getName() != null) {
                            ctx.distinctFonts.add(font.getName().replaceAll("^[A-Z]{6}\\+", ""));
                        }
                    }
                }
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            ctx.fullText = text;

            String[] lines = text.split("\n");
            int blankStreak = 0;
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    blankStreak++;
                    if (blankStreak >= 3) ctx.hasExcessiveBlankLines = true;
                } else {
                    blankStreak = 0;
                }
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && BULLET_CHAR_CATEGORIES.contains(String.valueOf(trimmed.charAt(0)))) {
                    ctx.bulletStyles.add(String.valueOf(trimmed.charAt(0)));
                }
            }

            // Alignment/spacing/heading-consistency need coordinate-level layout analysis,
            // which plain text stripping can't provide - deliberately left as single-value
            // (no penalty) rather than guessed. The AI layer above compensates for this gap
            // using text-pattern inference instead.
            ctx.alignmentVariants.add("UNKNOWN");
            ctx.paragraphSpacingVariants.add(0);
            ctx.headingsConsistentlyFormatted = true;
            ctx.headingCount = 1;

        } catch (Exception e) {
            throw new FormattingCheckException("Failed to analyze PDF formatting", e);
        }
        return ctx;
    }

    private FormattingIssueDTO issue(String category, String severity, String message, String suggestion, String source) {
        return FormattingIssueDTO.builder()
                .category(category)
                .severity(severity)
                .message(message)
                .suggestion(suggestion)
                .source(source)
                .build();
    }

    @Override
    public List<FormattingCheckResponseDTO> getHistory(String userId) {
        return formattingCheckResultRepository.findByUserIdOrderByCheckedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public FormattingCheckResponseDTO getById(String id, String userId) {
        FormattingCheckResult entity = formattingCheckResultRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new FormattingCheckException("Formatting check result not found"));
        return toDTO(entity);
    }

    private FormattingCheckResponseDTO toDTO(FormattingCheckResult entity) {
        return FormattingCheckResponseDTO.builder()
                .id(entity.getId())
                .resumeId(entity.getResumeId())
                .formattingScore(entity.getFormattingScore())
                .ruleBasedScore(entity.getRuleBasedScore())
                .aiAssessedScore(entity.getAiAssessedScore())
                .aiOverallImpression(entity.getAiOverallImpression())
                .issues(entity.getIssues())
                .suggestions(entity.getSuggestions())
                .distinctFontsUsed(entity.getDistinctFontsUsed())
                .distinctFontSizesUsed(entity.getDistinctFontSizesUsed())
                .bulletStyleVariantsUsed(entity.getBulletStyleVariantsUsed())
                .checkedAt(entity.getCheckedAt())
                .build();
    }

    private static class AnalysisContext {
        String fullText = "";
        Set<String> distinctFonts = new HashSet<>();
        Set<Integer> distinctFontSizes = new HashSet<>();
        Set<String> bulletStyles = new HashSet<>();
        Set<String> alignmentVariants = new HashSet<>();
        Set<Integer> paragraphSpacingVariants = new HashSet<>();
        Set<Integer> headingFontSizes = new HashSet<>();
        int headingCount = 0;
        boolean headingsConsistentlyFormatted = true;
        boolean hasExcessiveBlankLines = false;
    }

    // Internal helpers matching Gemini's raw JSON shape
    private static class AiFormattingIssueRaw {
        public String category;
        public String severity;
        public String message;
        public String suggestion;
    }

    private static class AiFormattingRaw {
        public Integer formattingScore;
        public String overallImpression;
        public List<AiFormattingIssueRaw> issues;
    }
}