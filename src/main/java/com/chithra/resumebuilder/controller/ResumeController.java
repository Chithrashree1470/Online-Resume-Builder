
package com.chithra.resumebuilder.controller;

import com.chithra.resumebuilder.model.Resume;
import com.chithra.resumebuilder.repository.ResumeRepository;
import com.chithra.resumebuilder.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.chithra.resumebuilder.service.PdfService;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class ResumeController {
    private final PdfService pdfService;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    public ResumeController(ResumeRepository resumeRepository,
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

        return "resume-form";
    }

    @PostMapping("/resume/save")
    public String saveResume(@ModelAttribute Resume resume,
                             Principal principal) {

        var user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        resume.setUser(user);

        resumeRepository.save(resume);

        return "redirect:/dashboard";
    }

    @GetMapping("/resume/{id}")
    public String viewResume(@PathVariable Long id,
                             Principal principal,
                             Model model) {

        Resume resume = resumeRepository
                .findByIdAndUserUsername(id, principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resume not found"));

        model.addAttribute("resume", resume);

        return "resume-view";
    }
    @GetMapping("/resume/{id}/download")
    public ResponseEntity<byte[]> downloadResume(
            @PathVariable Long id,
            Principal principal) throws IOException {

        Resume resume = resumeRepository
                .findByIdAndUserUsername(id, principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resume not found"));

        byte[] pdf = pdfService.generatePdf(resume);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=resume.pdf")
                .body(pdf);
    }
    @PostMapping("/resume/{id}/delete")
    public String deleteResume(@PathVariable Long id,
                               Principal principal) {

        Resume resume = resumeRepository
                .findByIdAndUserUsername(id, principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Resume not found"));

        resumeRepository.delete(resume);

        return "redirect:/dashboard";
    }
}
