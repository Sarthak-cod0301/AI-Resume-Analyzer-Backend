// controller/ResumeController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @PostMapping("/upload")
    public ResponseEntity<ResumeResponseDTO> upload(@RequestParam("file") MultipartFile file,
                                                      Authentication authentication) {
        ResumeResponseDTO dto = resumeService.uploadResume(file, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/replace")
    public ResponseEntity<ResumeResponseDTO> replace(@PathVariable String id,
                                                       @RequestParam("file") MultipartFile file,
                                                       Authentication authentication) {
        return ResponseEntity.ok(resumeService.replaceResume(id, file, currentUserId(authentication)));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponseDTO>> getAll(Authentication authentication) {
        return ResponseEntity.ok(resumeService.getAllResumes(currentUserId(authentication)));
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<ResumeResponseDTO> rename(@PathVariable String id,
                                                      @Valid @RequestBody RenameResumeRequestDTO request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(resumeService.renameResume(id, currentUserId(authentication), request.getResumeName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        resumeService.deleteResume(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String id, Authentication authentication) {
        String userId = currentUserId(authentication);
        byte[] data = resumeService.downloadResume(id, userId);
        String fileName = resumeService.getResumeFileName(id, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ByteArrayResource> preview(@PathVariable String id, Authentication authentication) {
        String userId = currentUserId(authentication);
        byte[] data = resumeService.previewResume(id, userId);
        String fileName = resumeService.getResumeFileName(id, userId);
        MediaType mediaType = fileName.toLowerCase().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ResumeVersionDTO>> versionHistory(@PathVariable String id,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(resumeService.getVersionHistory(id, currentUserId(authentication)));
    }

    @GetMapping("/{id}/versions/{versionNumber}/download")
    public ResponseEntity<ByteArrayResource> downloadVersion(@PathVariable String id,
                                                              @PathVariable Integer versionNumber,
                                                              Authentication authentication) {
        byte[] data = resumeService.downloadVersion(id, versionNumber, currentUserId(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"version-" + versionNumber + "\"")
                .body(new ByteArrayResource(data));
    }
}