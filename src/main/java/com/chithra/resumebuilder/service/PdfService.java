package com.chithra.resumebuilder.service;

import com.chithra.resumebuilder.model.Resume;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    // A4 page dimensions in PDF points.
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    // Equal margins on all four sides.
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    // Colours matched to resume-view.html.
    private static final Color BLUE = new Color(82, 105, 232);
    private static final Color LIGHT_BLUE = new Color(237, 240, 255);
    private static final Color TEXT = Color.BLACK;
    private static final Color MUTED = new Color(102, 112, 133);
    private static final Color TABLE_BORDER = Color.BLACK;

    private final PDType1Font bold =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    // Serif typography for body text, matching the View Resume preview.
    private final PDType1Font normal =
            new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

    private final PDType1Font bodyBold =
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

    private final PDType1Font italic =
            new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);

    public byte[] generatePdf(Resume resume) throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Writer writer = new Writer(document);

            writer.startPage();
            writer.drawHeader(resume);

            // Keep the same section order as resume-view.html.
            writer.writeTextSection(
                    "PROFESSIONAL SUMMARY", resume.getSummary());

            writer.writeTextSection(
                    "EXPERIENCE LEVEL", resume.getExperienceLevel());

            writer.writeBulletSection(
                    "SKILLS", resume.getSkills());

            writer.writeEducationTable(resume);

            writer.writeBulletSection(
                    "PROJECTS", resume.getProjects());

            writer.writeBulletSection(
                    "CERTIFICATIONS", resume.getCertifications());

            writer.finishPage();

            document.save(output);
            return output.toByteArray();
        }
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /*
     * Standard PDF fonts support WinAnsi characters.
     * Unsupported characters are replaced to prevent PDF generation errors.
     */
    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("[^\\x20-\\xFF]", " ");
    }

    private class Writer {

        private final PDDocument document;

        private PDPage page;
        private PDPageContentStream content;

        private float y;
        private int pageNumber;

        Writer(PDDocument document) {
            this.document = document;
        }

        void startPage() throws IOException {

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            content = new PDPageContentStream(document, page);
            pageNumber++;

            y = PAGE_HEIGHT - MARGIN;
            if (pageNumber > 1) {
                y -= 45f;
            }
        }

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        void drawHeader(Resume resume) throws IOException {

            float left = MARGIN;
            float textY = PAGE_HEIGHT - MARGIN - 27;

            // Leave space on the right for the profile photograph.
            float textWidth = CONTENT_WIDTH - 155;

            // Name: approximately the same visual size as the HTML preview.
            drawText(
                    safe(resume.getName()),
                    bold,
                    25,
                    Color.BLACK,
                    left,
                    textY);

            textY -= 25;

            if (!empty(resume.getEmail())) {
                drawText(
                        safe(resume.getEmail()),
                        normal,
                        10,
                        TEXT,
                        left,
                        textY);

                textY -= 16;
            }

            String phone =
                    (empty(resume.getCountryCode())
                            ? ""
                            : resume.getCountryCode() + " ")
                            + (resume.getPhone() == null
                            ? ""
                            : resume.getPhone());

            if (!empty(phone)) {
                drawText(
                        safe(phone),
                        normal,
                        10,
                        TEXT,
                        left,
                        textY);

                textY -= 16;
            }

            if (!empty(resume.getAddress())) {
                textY = drawWrappedText(
                        safe(resume.getAddress()),
                        normal,
                        10,
                        TEXT,
                        left,
                        textY,
                        textWidth,
                        13);

                textY -= 3;
            }

            if (!empty(resume.getLinkedin())) {
                addLinkLabel(
                        "LinkedIn",
                        resume.getLinkedin(),
                        left,
                        textY);

                textY -= 16;
            }

            if (!empty(resume.getGithub())) {
                addLinkLabel(
                        "GitHub",
                        resume.getGithub(),
                        left,
                        textY);

                textY -= 13;
            }

            float photoBottom = drawProfilePhoto(resume);

            // Place a divider under the first-page header (name/contact/photo).
            // No header or divider is repeated on continuation pages.
            y = Math.min(textY, photoBottom) - 18;

            // Avoid an excessive blank gap when no photograph is present.
            if (resume.getProfileImage() == null
                    || resume.getProfileImage().length == 0) {
                y = textY - 16;
            }
        }

        private float drawProfilePhoto(Resume resume) throws IOException {

            byte[] bytes = resume.getProfileImage();

            if (bytes == null || bytes.length == 0) {
                return PAGE_HEIGHT - MARGIN - 15;
            }

            try {
                PDImageXObject image =
                        PDImageXObject.createFromByteArray(
                                document,
                                bytes,
                                "profile-photo");

                // Larger portrait area, while preserving the original aspect ratio.
                float maxWidth = 130;
                float maxHeight = 150;

                // Keep the original aspect ratio. No crop or stretching.
                float scale = Math.min(
                        maxWidth / image.getWidth(),
                        maxHeight / image.getHeight());

                float width = image.getWidth() * scale;
                float height = image.getHeight() * scale;

                float x = PAGE_WIDTH - MARGIN - width;
                float top = PAGE_HEIGHT - MARGIN - 12;
                float photoY = top - height;

                content.drawImage(image, x, photoY, width, height);

                return photoY;

            } catch (IOException exception) {
                // Continue generating the resume if the photo cannot be read.
                return PAGE_HEIGHT - MARGIN - 15;
            }
        }

        private void addLinkLabel(
                String label,
                String url,
                float x,
                float yPosition) throws IOException {

            float fontSize = 10;

            // Italic, blue and underlined link label.
            drawText(
                    label,
                    italic,
                    fontSize,
                    BLUE,
                    x,
                    yPosition);

            float textWidth =
                    italic.getStringWidth(label) / 1000f * fontSize;

            // Draw the visible underline.
            content.setStrokingColor(BLUE);
            content.setLineWidth(0.6f);
            content.moveTo(x, yPosition - 2);
            content.lineTo(x + textWidth, yPosition - 2);
            content.stroke();

            try {
                URI uri = URI.create(url.trim());

                if (("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null) {

                    PDAnnotationLink link = new PDAnnotationLink();

                    link.setRectangle(
                            new PDRectangle(
                                    x,
                                    yPosition - 3,
                                    textWidth,
                                    15));

                    // Hide the default PDF hyperlink border.
                    COSArray border = new COSArray();
                    border.add(COSInteger.ZERO);
                    border.add(COSInteger.ZERO);
                    border.add(COSInteger.ZERO);

                    link.setBorder(border);

                    PDActionURI action = new PDActionURI();
                    action.setURI(url.trim());

                    link.setAction(action);
                    page.getAnnotations().add(link);
                }

            } catch (IllegalArgumentException ignored) {
                // Keep the label visible if the supplied URL is invalid.
            }
        }

        // ---------------------------------------------------------
        // PROFESSIONAL SUMMARY AND EXPERIENCE LEVEL
        // ---------------------------------------------------------

        void writeTextSection(
                String heading,
                String value) throws IOException {

            if (empty(value)) {
                return;
            }

            sectionHeading(heading);

            String[] paragraphs = value.split("\\R");

            for (String paragraph : paragraphs) {

                if (paragraph.trim().isEmpty()) {
                    continue;
                }

                List<String> lines = wrap(
                        paragraph.trim(),
                        normal,
                        10,
                        CONTENT_WIDTH);

                for (int i = 0; i < lines.size(); i++) {

                    ensureSpace(18);

                    // Justify all but the last line of each paragraph.
                    if (i < lines.size() - 1) {
                        drawJustifiedLine(
                                lines.get(i),
                                normal,
                                10,
                                TEXT,
                                MARGIN,
                                y,
                                CONTENT_WIDTH);
                    } else {
                        drawText(
                                lines.get(i),
                                normal,
                                10,
                                TEXT,
                                MARGIN,
                                y);
                    }

                    y -= 17;
                }
            }

            y -= 7;
        }

        // ---------------------------------------------------------
        // SKILLS, PROJECTS AND CERTIFICATIONS
        // ---------------------------------------------------------

        void writeBulletSection(
                String heading,
                String value) throws IOException {

            if (empty(value)) {
                return;
            }

            sectionHeading(heading);

            // Exactly one bullet for each entered line / Enter.
            String[] items = value.split("\\R");

            for (String raw : items) {

                String item = raw.trim();

                if (!item.isEmpty()) {
                    writeBullet(item);
                }
            }

            y -= 9;
        }

        private void writeBullet(String item) throws IOException {

            List<String> lines = wrap(
                    item,
                    normal,
                    10,
                    CONTENT_WIDTH - 25);

            for (int i = 0; i < lines.size(); i++) {

                ensureSpace(18);

                if (i == 0) {
                    drawBulletDot(MARGIN + 5, y + 3);
                }

                String line = lines.get(i);

                // Match the preview: wrapped text remains under its bullet.
                if (i < lines.size() - 1) {
                    drawJustifiedLine(
                            line,
                            normal,
                            10,
                            TEXT,
                            MARGIN + 18,
                            y,
                            CONTENT_WIDTH - 18);
                } else {
                    drawText(
                            line,
                            normal,
                            10,
                            TEXT,
                            MARGIN + 18,
                            y);
                }

                y -= 17;
            }

            // Small gap between separate bullet items.
            y -= 2;
        }

        private void drawBulletDot(float centerX, float centerY)
                throws IOException {

            float radius = 1.7f;

            content.setNonStrokingColor(TEXT);

            // Draw a small circular bullet.
            content.moveTo(centerX + radius, centerY);

            content.curveTo(
                    centerX + radius, centerY + radius * 0.55f,
                    centerX + radius * 0.55f, centerY + radius,
                    centerX, centerY + radius);

            content.curveTo(
                    centerX - radius * 0.55f, centerY + radius,
                    centerX - radius, centerY + radius * 0.55f,
                    centerX - radius, centerY);

            content.curveTo(
                    centerX - radius, centerY - radius * 0.55f,
                    centerX - radius * 0.55f, centerY - radius,
                    centerX, centerY - radius);

            content.curveTo(
                    centerX + radius * 0.55f, centerY - radius,
                    centerX + radius, centerY - radius * 0.55f,
                    centerX + radius, centerY);

            content.fill();
        }

        // ---------------------------------------------------------
        // EDUCATION TABLE
        // ---------------------------------------------------------

        void writeEducationTable(Resume resume) throws IOException {

            List<String[]> rows = new ArrayList<>();

            addEducationRow(
                    rows,
                    "School",
                    resume.getSchoolQualification(),
                    resume.getSchoolYear(),
                    resume.getSchoolPercentage());

            addEducationRow(
                    rows,
                    "PU / Higher Secondary",
                    resume.getPuQualification(),
                    resume.getPuYear(),
                    resume.getPuPercentage());

            addEducationRow(
                    rows,
                    "Degree",
                    resume.getDegreeQualification(),
                    resume.getDegreeYear(),
                    resume.getDegreePercentage());

            if (rows.isEmpty() && empty(resume.getEducation())) {
                return;
            }

            sectionHeading("EDUCATION");

            float[] widths = {
                    105,
                    205,
                    65,
                    CONTENT_WIDTH - 375
            };

            drawTableRow(
                    new String[]{
                            "Level",
                            "Qualification / Institution",
                            "Year",
                            "Percentage / CGPA"
                    },
                    widths,
                    true);

            for (String[] row : rows) {
                drawTableRow(row, widths, false);
            }

            if (!empty(resume.getEducation())) {
                y -= 7;
                writeTextSection(
                        "ADDITIONAL EDUCATION",
                        resume.getEducation());
            }

            // Match the modest gap in the HTML preview.
            y -= 12;
        }

        private void addEducationRow(
                List<String[]> rows,
                String level,
                String qualification,
                String year,
                String percentage) {

            if (!empty(qualification)
                    || !empty(year)
                    || !empty(percentage)) {

                rows.add(new String[]{
                        level,
                        safe(qualification),
                        safe(year),
                        safe(percentage)
                });
            }
        }

        private void drawTableRow(
                String[] cells,
                float[] widths,
                boolean header) throws IOException {

            List<List<String>> wrappedCells = new ArrayList<>();
            int maxLines = 1;

            for (int i = 0; i < cells.length; i++) {

                List<String> lines = wrap(
                        safe(cells[i]),
                        header ? bold : normal,
                        8,
                        widths[i] - 10);

                if (lines.isEmpty()) {
                    lines = List.of("");
                }

                wrappedCells.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }

            float rowHeight = Math.max(25, maxLines * 12 + 10);

            ensureSpace(rowHeight + 2);

            float x = MARGIN;
            float bottom = y - rowHeight + 7;

            for (int i = 0; i < cells.length; i++) {

                // The HTML preview uses a light-blue table header.
                content.setNonStrokingColor(
                        header ? LIGHT_BLUE : Color.WHITE);

                content.addRect(
                        x,
                        bottom,
                        widths[i],
                        rowHeight);

                content.fill();

                content.setStrokingColor(TABLE_BORDER);
                content.setLineWidth(0.5f);

                content.addRect(
                        x,
                        bottom,
                        widths[i],
                        rowHeight);

                content.stroke();

                float textY = y - 9;

                for (String line : wrappedCells.get(i)) {

                    drawText(
                            line,
                            header ? bodyBold : normal,
                            8.5f,
                            header ? BLUE : TEXT,
                            x + 5,
                            textY);

                    textY -= 12;
                }

                x += widths[i];
            }

            y -= rowHeight;
        }

        // ---------------------------------------------------------
        // SECTION HEADING
        // ---------------------------------------------------------

        private void sectionHeading(String heading) throws IOException {

            ensureSpace(38);

            // Similar to the blue uppercase headings in the HTML preview.
            drawText(
                    heading,
                    bold,
                    12,
                    BLUE,
                    MARGIN,
                    y);

            y -= 7;

            content.setStrokingColor(TABLE_BORDER);
            content.setLineWidth(0.6f);

            content.moveTo(MARGIN, y);
            content.lineTo(PAGE_WIDTH - MARGIN, y);
            content.stroke();

            y -= 17;
        }

        // ---------------------------------------------------------
        // TEXT WRAPPING AND JUSTIFICATION
        // ---------------------------------------------------------

        private float drawWrappedText(
                String value,
                PDType1Font font,
                float size,
                Color color,
                float x,
                float startY,
                float maxWidth,
                float lineHeight) throws IOException {

            float currentY = startY;

            for (String line : wrap(value, font, size, maxWidth)) {

                drawText(
                        line,
                        font,
                        size,
                        color,
                        x,
                        currentY);

                currentY -= lineHeight;
            }

            return currentY;
        }

        private void drawJustifiedLine(
                String line,
                PDType1Font font,
                float size,
                Color color,
                float x,
                float yPosition,
                float maxWidth) throws IOException {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                return;
            }

            String[] words = trimmed.split("\\s+");

            if (words.length <= 1) {
                drawText(
                        trimmed,
                        font,
                        size,
                        color,
                        x,
                        yPosition);
                return;
            }

            float actualWidth =
                    font.getStringWidth(trimmed) / 1000f * size;

            float extraWordSpacing =
                    (maxWidth - actualWidth) / (words.length - 1);

            extraWordSpacing = Math.max(0, extraWordSpacing);

            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, yPosition);
            content.setWordSpacing(extraWordSpacing);
            content.showText(trimmed);
            content.setWordSpacing(0);
            content.endText();
        }

        private List<String> wrap(
                String value,
                PDType1Font font,
                float size,
                float maxWidth) throws IOException {

            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();

            String cleaned = safe(value).replace('\t', ' ');

            for (String word : cleaned.trim().split("\\s+")) {

                if (word.isEmpty()) {
                    continue;
                }

                String candidate = currentLine.length() == 0
                        ? word
                        : currentLine + " " + word;

                float candidateWidth =
                        font.getStringWidth(candidate) / 1000f * size;

                if (candidateWidth > maxWidth
                        && currentLine.length() > 0) {

                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);

                } else {
                    currentLine = new StringBuilder(candidate);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            return lines;
        }

        private void ensureSpace(float required) throws IOException {

            // Keep all content inside the bottom margin and above the footer.
            if (y - required < MARGIN + 25) {
                finishPage();
                startPage();
            }
        }

        // ---------------------------------------------------------
        // BASIC TEXT DRAWING
        // ---------------------------------------------------------

        private void drawText(
                String value,
                PDType1Font font,
                float size,
                Color color,
                float x,
                float yPosition) throws IOException {

            if (value == null || value.isEmpty()) {
                return;
            }

            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, yPosition);
            content.showText(safe(value));
            content.endText();
        }

        // ---------------------------------------------------------
        // PAGE FOOTER
        // ---------------------------------------------------------

        void finishPage() throws IOException {

            // Footer line and page number remain inside the page margins.
            content.setStrokingColor(TABLE_BORDER);
            content.setLineWidth(0.6f);

            content.moveTo(MARGIN, MARGIN + 18);
            content.lineTo(PAGE_WIDTH - MARGIN, MARGIN + 18);
            content.stroke();

            drawText(
                    "RESUME",
                    normal,
                    8,
                    MUTED,
                    MARGIN,
                    MARGIN + 5);

            drawText(
                    "Page " + pageNumber,
                    normal,
                    8,
                    MUTED,
                    PAGE_WIDTH - MARGIN - 35,
                    MARGIN + 5);

            content.close();
        }
    }
}
