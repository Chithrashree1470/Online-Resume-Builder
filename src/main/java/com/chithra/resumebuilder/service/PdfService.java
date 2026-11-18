
package com.chithra.resumebuilder.service;

import com.chithra.resumebuilder.model.Resume;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfService {

    private static final float PAGE_WIDTH = 595;
    private static final float PAGE_HEIGHT = 842;
    private static final float LEFT = 55;
    private static final float RIGHT = 55;
    private static final float BOTTOM = 55;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;

    private static final Color NAVY = new Color(23, 33, 59);
    private static final Color BLUE = new Color(82, 105, 232);
    private static final Color LIGHT_BLUE = new Color(237, 240, 255);
    private static final Color TEXT = new Color(65, 75, 95);
    private static final Color MUTED = new Color(185, 197, 220);

    private final PDType1Font bold =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final PDType1Font normal =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private final PDType1Font italic =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public byte[] generatePdf(Resume resume) throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(document);

            writer.startPage(true, resume);


            writer.writeSection("SKILLS", resume.getSkills());
            writer.writeSection("EDUCATION", resume.getEducation());
            writer.writeSection("PROJECTS", resume.getProjects());

            writer.finishPage();

            document.save(output);

            return output.toByteArray();
        }
    }

    private class PdfWriter {

        private final PDDocument document;
        private PDPageContentStream content;
        private float y;
        private int pageNumber = 0;

        PdfWriter(PDDocument document) {
            this.document = document;
        }

        void startPage(boolean firstPage, Resume resume)
                throws IOException {

            pageNumber++;

            PDPage page = new PDPage();
            document.addPage(page);

            content = new PDPageContentStream(document, page);

            if (firstPage) {
                drawHeader(resume);
                y = 630;
            } else {
                drawSmallHeader();
                y = 760;
            }
        }



        private void drawHeader(Resume resume) throws IOException {

            // Get the actual width of the PDF page
            float pageWidth = document
                    .getPage(pageNumber - 1)
                    .getMediaBox()
                    .getWidth();

            // Full-width navy header
            content.setNonStrokingColor(NAVY);
            content.addRect(0, 662, pageWidth, 180);
            content.fill();

            // Name - moved down to prevent clipping
            drawText(safe(resume.getName()),
                    bold, 30, Color.WHITE, LEFT, 735);

            // Contact details
            drawText(safe(resume.getEmail()),
                    normal, 12, MUTED, LEFT, 700);

            drawText(safe(resume.getPhone()),
                    normal, 12, MUTED, LEFT, 680);


        }



        private void drawSmallHeader() throws IOException {

            content.setNonStrokingColor(NAVY);
            content.addRect(0, 795, PAGE_WIDTH, 47);
            content.fill();

            drawText("RESUME", bold, 15,
                    Color.WHITE, LEFT, 812);
        }

        private void drawText(String value,
                              PDType1Font font,
                              float fontSize,
                              Color color,
                              float x,
                              float y) throws IOException {

            content.beginText();
            content.setFont(font, fontSize);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, y);
            content.showText(safe(value));
            content.endText();
        }

        void writeSection(String heading, String value)
                throws IOException {

            if (value == null || value.trim().isEmpty()) {
                return;
            }

            ensureSpace(65);

            // Section heading background
            content.setNonStrokingColor(LIGHT_BLUE);
            content.addRect(LEFT, y - 8, CONTENT_WIDTH, 27);
            content.fill();

            drawText(heading, bold, 12, BLUE,
                    LEFT + 10, y);

            y -= 38;

            String[] paragraphs = value.split("\\R");

            for (String paragraph : paragraphs) {

                if (paragraph.trim().isEmpty()) {
                    y -= 8;
                    continue;
                }

                writeWrappedText(paragraph.trim(), normal, 11, TEXT);

                y -= 5;
            }

            y -= 15;
        }

        private void writeWrappedText(String text,
                                      PDType1Font font,
                                      float fontSize,
                                      Color color) throws IOException {

            String[] words = safe(text).split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {

                String candidate = line.length() == 0
                        ? word
                        : line + " " + word;

                float width = font.getStringWidth(candidate)
                        / 1000 * fontSize;

                if (width > CONTENT_WIDTH && line.length() > 0) {

                    writeLine(line.toString(), font, fontSize, color);
                    line = new StringBuilder(word);

                } else {
                    line = new StringBuilder(candidate);
                }
            }

            if (line.length() > 0) {
                writeLine(line.toString(), font, fontSize, color);
            }
        }

        private void writeLine(String line,
                               PDType1Font font,
                               float fontSize,
                               Color color) throws IOException {

            ensureSpace(20);

            drawText(line, font, fontSize,
                    color, LEFT + 5, y);

            y -= 18;
        }

        private void ensureSpace(float required)
                throws IOException {

            if (y - required < BOTTOM) {
                finishPage();
                startPage(false, null);
            }
        }

        void finishPage() throws IOException {

            // Footer line
            content.setStrokingColor(MUTED);
            content.moveTo(LEFT, 40);
            content.lineTo(PAGE_WIDTH - RIGHT, 40);
            content.stroke();

            drawText("RESUME BUILDER",
                    italic, 8, TEXT, LEFT, 25);

            drawText("Page " + pageNumber,
                    normal, 8, TEXT,
                    PAGE_WIDTH - RIGHT - 40, 25);

            content.close();
        }
    }

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
