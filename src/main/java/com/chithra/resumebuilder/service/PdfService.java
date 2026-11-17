
package com.chithra.resumebuilder.service;

import com.chithra.resumebuilder.model.Resume;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

@Service
public class PdfService {

    public byte[] generatePdf(Resume resume) throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font bold = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD);

            PDType1Font normal = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float y = 750;

                content.beginText();
                content.setFont(bold, 22);
                content.newLineAtOffset(50, y);
                content.showText(safe(resume.getName()));
                content.endText();

                y -= 35;

                y = writeLine(content, normal, "Email: " + safe(resume.getEmail()), y);
                y = writeLine(content, normal, "Phone: " + safe(resume.getPhone()), y);

                y -= 15;

                y = writeSection(content, bold, normal,
                        "SKILLS", resume.getSkills(), y);

                y = writeSection(content, bold, normal,
                        "EDUCATION", resume.getEducation(), y);

                writeSection(content, bold, normal,
                        "EXPERIENCE", resume.getExperience(), y);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private float writeSection(PDPageContentStream content,
                               PDType1Font bold,
                               PDType1Font normal,
                               String heading,
                               String value,
                               float y) throws IOException {

        content.beginText();
        content.setFont(bold, 14);
        content.newLineAtOffset(50, y);
        content.showText(heading);
        content.endText();

        y -= 25;

        for (String line : safe(value).split("\\R")) {
            y = writeLine(content, normal, line, y);
        }

        return y - 20;
    }

    private float writeLine(PDPageContentStream content,
                            PDType1Font font,
                            String value,
                            float y) throws IOException {

        content.beginText();
        content.setFont(font, 11);
        content.newLineAtOffset(50, y);
        content.showText(safe(value));
        content.endText();

        return y - 18;
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
