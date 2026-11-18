
package com.chithra.resumebuilder.controller;

import com.chithra.resumebuilder.model.Resume;
import com.chithra.resumebuilder.repository.ResumeRepository;
import com.chithra.resumebuilder.repository.UserRepository;
import com.chithra.resumebuilder.service.PdfService;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ResumeController {

    private static final long MAX_PHOTO_SIZE = 10L * 1024 * 1024;

    private final PdfService pdfService;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    // Split textarea content at each Enter.
    private List<String> splitLines(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    public ResumeController(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            PdfService pdfService) {

        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.pdfService = pdfService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {

        model.addAttribute("username", principal.getName());

        model.addAttribute("resumes",
                resumeRepository.findByUserUsernameOrderByIdDesc(
                        principal.getName()));

        return "dashboard";
    }

    @GetMapping("/resume/new")
    public String newResume(Model model) {

        model.addAttribute("resume", new Resume());
        model.addAttribute("editMode", false);

        return "resume-form";
    }

    @PostMapping("/resume/save")
    public String saveResume(
            @ModelAttribute Resume resume,
            @RequestParam(value = "profilePhoto", required = false)
            MultipartFile profilePhoto,
            Principal principal) throws IOException {

        validateAndSetPhoto(resume, profilePhoto);

        var user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        resume.setUser(user);
        resumeRepository.save(resume);

        return "redirect:/dashboard";
    }

    @GetMapping("/resume/{id}/edit")
    public String editResume(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        Resume resume = findOwnedResume(id, principal);

        model.addAttribute("resume", resume);
        model.addAttribute("editMode", true);

        return "resume-form";
    }

    @PostMapping("/resume/{id}/edit")
    public String updateResume(
            @PathVariable Long id,
            @ModelAttribute Resume formResume,
            @RequestParam(value = "profilePhoto", required = false)
            MultipartFile profilePhoto,
            Principal principal) throws IOException {

        Resume existing = findOwnedResume(id, principal);

        existing.setName(formResume.getName());
        existing.setAddress(formResume.getAddress());
        existing.setEmail(formResume.getEmail());
        existing.setCountryCode(formResume.getCountryCode());
        existing.setPhone(formResume.getPhone());

        existing.setLinkedin(formResume.getLinkedin());
        existing.setGithub(formResume.getGithub());

        existing.setSummary(formResume.getSummary());
        existing.setSkills(formResume.getSkills());
        existing.setProjects(formResume.getProjects());
        existing.setCertifications(formResume.getCertifications());
        existing.setExperienceLevel(formResume.getExperienceLevel());

        existing.setEducation(formResume.getEducation());

        existing.setSchoolQualification(formResume.getSchoolQualification());
        existing.setSchoolYear(formResume.getSchoolYear());
        existing.setSchoolPercentage(formResume.getSchoolPercentage());

        existing.setPuQualification(formResume.getPuQualification());
        existing.setPuYear(formResume.getPuYear());
        existing.setPuPercentage(formResume.getPuPercentage());

        existing.setDegreeQualification(formResume.getDegreeQualification());
        existing.setDegreeYear(formResume.getDegreeYear());
        existing.setDegreePercentage(formResume.getDegreePercentage());

        // Keep the old photo if no new photo is selected.
        validateAndSetPhoto(existing, profilePhoto);

        resumeRepository.save(existing);

        return "redirect:/resume/" + id;
    }

    private void validateAndSetPhoto(
            Resume resume,
            MultipartFile photo) throws IOException {

        if (photo == null || photo.isEmpty()) {
            return;
        }

        if (photo.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException(
                    "Photo must be smaller than 10 MB.");
        }

        String contentType = photo.getContentType();

        if (!MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {

            throw new IllegalArgumentException(
                    "Only JPG and PNG photographs are allowed.");
        }

        resume.setProfileImage(photo.getBytes());
        resume.setProfileImageContentType(contentType.toLowerCase());
    }

    @GetMapping("/resume/{id}")
    public String viewResume(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        Resume resume = findOwnedResume(id, principal);

        model.addAttribute("resume", resume);

        // These lists create one bullet for each entered line.
        model.addAttribute("skillsList",
                splitLines(resume.getSkills()));

        model.addAttribute("projectsList",
                splitLines(resume.getProjects()));

        model.addAttribute("certificationsList",
                splitLines(resume.getCertifications()));

        return "resume-view";
    }

    @GetMapping("/resume/{id}/photo")
    public ResponseEntity<byte[]> getResumePhoto(
            @PathVariable Long id,
            Principal principal) {

        Resume resume = findOwnedResume(id, principal);
        byte[] photo = resume.getProfileImage();

        if (photo == null || photo.length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType type = MediaType.IMAGE_JPEG;

        if (MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(
                resume.getProfileImageContentType())) {
            type = MediaType.IMAGE_PNG;
        }

        return ResponseEntity.ok()
                .contentType(type)
                .body(photo);
    }

    @GetMapping("/resume/{id}/download")
    public ResponseEntity<byte[]> downloadResume(
            @PathVariable Long id,
            Principal principal) throws IOException {

        Resume resume = findOwnedResume(id, principal);
        byte[] pdf = pdfService.generatePdf(resume);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=resume.pdf")
                .body(pdf);
    }

    @PostMapping("/resume/{id}/delete")
    public String deleteResume(
            @PathVariable Long id,
            Principal principal) {

        resumeRepository.delete(findOwnedResume(id, principal));

        return "redirect:/dashboard";
    }

    private Resume findOwnedResume(Long id, Principal principal) {

        return resumeRepository
                .findByIdAndUserUsername(id, principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resume not found"));
    }
}
