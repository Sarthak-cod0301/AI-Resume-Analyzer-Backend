package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.Resume;
import com.example.demo.entity.ResumeVersion;
import com.example.demo.exception.ResumeNotFoundException;
import com.example.demo.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";

    private final ResumeRepository resumeRepository;
    private final GridFSService gridFSService;

    @Override
    public ResumeResponseDTO uploadResume(MultipartFile file, String userId) {
        String fileId;
        try {
            fileId = gridFSService.saveFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store resume", e);
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.') + 1)
                : "";
        String storedFileName = originalName;

        ResumeVersion firstVersion = ResumeVersion.builder()
                .versionNumber(1)
                .storedFileName(storedFileName)
                .filePath(fileId)
                .originalFileName(originalName)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();

        Resume resume = Resume.builder()
                .userId(userId)
                .resumeName(originalName)
                .storedFileName(storedFileName)
                .resumePath(fileId)
                .fileType(ext)
                .fileSize(file.getSize())
                .status(STATUS_ACTIVE)
                .uploadDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        resume.getVersions().add(firstVersion);
        resume = resumeRepository.save(resume);

        return toDTO(resume);
    }

    @Override
    public ResumeResponseDTO replaceResume(String resumeId, MultipartFile file, String userId) {
        Resume resume = getOwnedResume(resumeId, userId);

        int nextVersion = resume.getVersions().stream()
                .mapToInt(ResumeVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;

        String fileId;
        try {
            fileId = gridFSService.saveFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store resume", e);
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.') + 1)
                : "";
        String storedFileName = originalName;

        ResumeVersion newVersion = ResumeVersion.builder()
                .versionNumber(nextVersion)
                .storedFileName(storedFileName)
                .filePath(fileId)
                .originalFileName(originalName)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();

        resume.getVersions().add(newVersion);
        resume.setStoredFileName(storedFileName);
        resume.setResumePath(fileId);
        resume.setFileType(ext);
        resume.setFileSize(file.getSize());
        resume.setUpdatedAt(LocalDateTime.now());

        return toDTO(resumeRepository.save(resume));
    }

    @Override
    public List<ResumeResponseDTO> getAllResumes(String userId) {
        return resumeRepository.findByUserIdAndStatus(userId, STATUS_ACTIVE)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ResumeResponseDTO renameResume(String resumeId, String userId, String newName) {
        Resume resume = getOwnedResume(resumeId, userId);
        resume.setResumeName(newName);
        resume.setUpdatedAt(LocalDateTime.now());
        return toDTO(resumeRepository.save(resume));
    }

    @Override
    public void deleteResume(String resumeId, String userId) {
        Resume resume = getOwnedResume(resumeId, userId);

        resume.getVersions().forEach(v ->
                gridFSService.deleteFile(v.getFilePath()));

        resume.setStatus(STATUS_DELETED);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeRepository.save(resume);
    }

    @Override
    public byte[] downloadResume(String resumeId, String userId) {
        try {
            Resume resume = getOwnedResume(resumeId, userId);
            return gridFSService.getFile(resume.getResumePath())
                    .getInputStream()
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download resume", e);
        }
    }

    @Override
    public String getResumeFileName(String resumeId, String userId) {
        return getOwnedResume(resumeId, userId).getResumeName();
    }

    @Override
    public List<ResumeVersionDTO> getVersionHistory(String resumeId, String userId) {
        Resume resume = getOwnedResume(resumeId, userId);
        return resume.getVersions().stream()
                .sorted(Comparator.comparing(ResumeVersion::getVersionNumber).reversed())
                .map(v -> ResumeVersionDTO.builder()
                        .versionNumber(v.getVersionNumber())
                        .originalFileName(v.getOriginalFileName())
                        .fileSize(v.getFileSize())
                        .createdAt(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public byte[] downloadVersion(String resumeId, Integer versionNumber, String userId) {
        Resume resume = getOwnedResume(resumeId, userId);
        ResumeVersion version = resume.getVersions().stream()
                .filter(v -> v.getVersionNumber().equals(versionNumber))
                .findFirst()
                .orElseThrow(() -> new ResumeNotFoundException("Version not found: " + versionNumber));
        try {
            return gridFSService.getFile(version.getFilePath())
                    .getInputStream()
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download version", e);
        }
    }

    @Override
    public byte[] previewResume(String resumeId, String userId) {
        try {
            Resume resume = getOwnedResume(resumeId, userId);
            return gridFSService.getFile(resume.getResumePath())
                    .getInputStream()
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to preview resume", e);
        }
    }

    private Resume getOwnedResume(String resumeId, String userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .filter(r -> STATUS_ACTIVE.equals(r.getStatus()))
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found with id: " + resumeId));
    }

    private ResumeResponseDTO toDTO(Resume resume) {
        return ResumeResponseDTO.builder()
                .id(resume.getId())
                .resumeName(resume.getResumeName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .status(resume.getStatus())
                .uploadDate(resume.getUploadDate())
                .updatedAt(resume.getUpdatedAt())
                .totalVersions(resume.getVersions().size())
                .build();
    }
}
