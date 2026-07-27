// service/DashboardServiceImpl.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.AtsCheckResult;
import com.example.demo.entity.JobDescription;
import com.example.demo.entity.Resume;
import com.example.demo.entity.ResumeAnalysis;
import com.example.demo.exception.DashboardException;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AtsCheckResultRepository atsCheckResultRepository;

    @Override
    public DashboardSummaryDTO getDashboard(String userId) {
        try {
            long totalResumes = resumeRepository.countByUserIdAndStatus(userId, STATUS_ACTIVE);
            long totalAnalyses = resumeAnalysisRepository.countByUserId(userId);

            List<AtsCheckResult> atsResults = atsCheckResultRepository.findByUserIdOrderByCheckedAtAsc(userId);
            List<ResumeAnalysis> analyses = resumeAnalysisRepository.findByUserIdOrderByAnalyzedAtAsc(userId);

            Double avgAtsScore = atsResults.isEmpty() ? 0.0 :
                    Math.round(atsResults.stream().mapToInt(AtsCheckResult::getAtsScore).average().orElse(0.0) * 10.0) / 10.0;

            Integer highestMatchScore = analyses.stream()
                    .mapToInt(ResumeAnalysis::getMatchScore)
                    .max()
                    .orElse(0);

            DashboardSummaryDTO.RecentResumeDTO recentResume = resumeRepository
                    .findFirstByUserIdAndStatusOrderByUploadDateDesc(userId, STATUS_ACTIVE)
                    .map(this::toRecentResumeDTO)
                    .orElse(null);

            DashboardSummaryDTO.RecentJobDescriptionDTO recentJd = jobDescriptionRepository
                    .findFirstByUserIdOrderByCreatedDateDesc(userId)
                    .map(this::toRecentJdDTO)
                    .orElse(null);

            List<ScoreTrendPointDTO> scoreTrend = buildScoreTrend(analyses);
            List<SkillGapItemDTO> skillGap = buildSkillGap(analyses);
            List<AtsImprovementPointDTO> atsImprovement = buildAtsImprovement(atsResults);

            return DashboardSummaryDTO.builder()
                    .totalResumes(totalResumes)
                    .totalAnalyses(totalAnalyses)
                    .averageAtsScore(avgAtsScore)
                    .highestMatchScore(highestMatchScore)
                    .recentResume(recentResume)
                    .recentJobDescription(recentJd)
                    .resumeScoreTrend(scoreTrend)
                    .skillGap(skillGap)
                    .atsImprovement(atsImprovement)
                    .build();

        } catch (Exception e) {
            throw new DashboardException("Failed to build dashboard data", e);
        }
    }

    private DashboardSummaryDTO.RecentResumeDTO toRecentResumeDTO(Resume resume) {
        return DashboardSummaryDTO.RecentResumeDTO.builder()
                .id(resume.getId())
                .resumeName(resume.getResumeName())
                .uploadDate(resume.getUploadDate())
                .build();
    }

    private DashboardSummaryDTO.RecentJobDescriptionDTO toRecentJdDTO(JobDescription jd) {
        return DashboardSummaryDTO.RecentJobDescriptionDTO.builder()
                .id(jd.getId())
                .title(jd.getTitle())
                .company(jd.getCompany())
                .createdDate(jd.getCreatedDate())
                .build();
    }

    // ---------- Chart 1: Resume Score Trend ----------
    private List<ScoreTrendPointDTO> buildScoreTrend(List<ResumeAnalysis> analyses) {
        return analyses.stream()
                .map(a -> ScoreTrendPointDTO.builder()
                        .date(a.getAnalyzedAt())
                        .matchScore(a.getMatchScore())
                        .resumeId(a.getResumeId())
                        .jobDescriptionId(a.getJobDescriptionId())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- Chart 2: Skill Gap ----------
    // missingSkills is a native embedded List<String> on ResumeAnalysis (Mongo) -
    // no JSON parsing needed, unlike the MySQL version's missingSkillsJson column.
    private List<SkillGapItemDTO> buildSkillGap(List<ResumeAnalysis> analyses) {
        Map<String, Integer> skillFrequency = new HashMap<>();

        for (ResumeAnalysis analysis : analyses) {
            List<String> missingSkills = analysis.getMissingSkills();
            if (missingSkills == null) continue;

            for (String skill : missingSkills) {
                String normalized = skill.trim();
                if (!normalized.isEmpty()) {
                    skillFrequency.merge(normalized, 1, Integer::sum);
                }
            }
        }

        return skillFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> SkillGapItemDTO.builder()
                        .skillName(entry.getKey())
                        .timesMissing(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- Chart 3: ATS Improvement ----------
    private List<AtsImprovementPointDTO> buildAtsImprovement(List<AtsCheckResult> results) {
        return results.stream()
                .map(r -> AtsImprovementPointDTO.builder()
                        .date(r.getCheckedAt())
                        .atsScore(r.getAtsScore())
                        .resumeId(r.getResumeId())
                        .build())
                .collect(Collectors.toList());
    }
}