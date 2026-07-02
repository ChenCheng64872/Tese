from pathlib import Path
import re
import html

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

BASE_DIR = Path(r"c:\Tese\results\JupterNoteBook")
MARKDOWN_PATH = BASE_DIR / "benchmark_analysis_report.md"
PDF_PATH = BASE_DIR / "benchmark_analysis_report.pdf"

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="TitleCenter", parent=styles["Title"], alignment=TA_CENTER, spaceAfter=18))
styles.add(ParagraphStyle(name="SectionHeading", parent=styles["Heading1"], spaceBefore=10, spaceAfter=8))
styles.add(ParagraphStyle(name="SubHeading", parent=styles["Heading2"], spaceBefore=8, spaceAfter=6))
styles.add(ParagraphStyle(name="Body", parent=styles["BodyText"], leading=13, spaceAfter=5))
styles.add(ParagraphStyle(name="BulletBody", parent=styles["BodyText"], leftIndent=14, bulletIndent=0, leading=13, spaceAfter=2))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontSize=9, leading=11, spaceAfter=4))


def esc(text: str) -> str:
    text = html.escape(text)
    text = text.replace("`", "")
    text = re.sub(r"\[(.*?)\]\((.*?)\)", r'<link href="\2">\1</link>', text)
    return text


def make_paragraph(text: str, style_name: str = "Body"):
    return Paragraph(esc(text), styles[style_name])


def parse_table(lines, start_index):
    header = [cell.strip() for cell in lines[start_index].strip().strip("|").split("|")]
    rows = []
    index = start_index + 2
    while index < len(lines):
        line = lines[index].strip()
        if not line.startswith("|"):
            break
        rows.append([cell.strip() for cell in line.strip().strip("|").split("|")])
        index += 1
    table_data = [header] + rows
    return table_data, index


def build_story(markdown_text: str):
    lines = markdown_text.splitlines()
    story = []

    story.append(Paragraph("Android/Mobile Cryptography Benchmark Analysis Report", styles["TitleCenter"]))
    story.append(Spacer(1, 0.15 * inch))

    i = 0
    paragraph_buffer = []

    def flush_paragraph_buffer():
        nonlocal paragraph_buffer
        if paragraph_buffer:
            text = " ".join(part.strip() for part in paragraph_buffer if part.strip())
            if text:
                story.append(make_paragraph(text, "Body"))
            paragraph_buffer = []

    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()

        if not stripped:
            flush_paragraph_buffer()
            story.append(Spacer(1, 0.08 * inch))
            i += 1
            continue

        if stripped.startswith("# "):
            flush_paragraph_buffer()
            story.append(Paragraph(esc(stripped[2:].strip()), styles["SectionHeading"]))
            i += 1
            continue
        if stripped.startswith("## "):
            flush_paragraph_buffer()
            story.append(Paragraph(esc(stripped[3:].strip()), styles["SectionHeading"]))
            i += 1
            continue
        if stripped.startswith("### "):
            flush_paragraph_buffer()
            story.append(Paragraph(esc(stripped[4:].strip()), styles["SubHeading"]))
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < len(lines) and re.match(r"^\|?\s*:?-{3,}", lines[i + 1].strip()):
            flush_paragraph_buffer()
            table_data, next_index = parse_table(lines, i)
            table = Table(table_data, repeatRows=1)
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#2F4F4F")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 8.5),
                ("LEADING", (0, 0), (-1, -1), 10),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.grey),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.whitesmoke, colors.HexColor("#EAF2F8")]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]))
            story.append(table)
            story.append(Spacer(1, 0.10 * inch))
            i = next_index
            continue

        if stripped.startswith("- "):
            flush_paragraph_buffer()
            story.append(Paragraph("• " + esc(stripped[2:].strip()), styles["BulletBody"]))
            i += 1
            continue

        if re.match(r"^\d+\.\s+", stripped):
            flush_paragraph_buffer()
            story.append(Paragraph(esc(stripped), styles["BulletBody"]))
            i += 1
            continue

        if stripped.startswith("The ") or stripped.startswith("If "):
            flush_paragraph_buffer()
            story.append(make_paragraph(stripped, "Body"))
            i += 1
            continue

        paragraph_buffer.append(stripped)
        i += 1

    flush_paragraph_buffer()
    return story


def main():
    markdown_text = MARKDOWN_PATH.read_text(encoding="utf-8")
    story = build_story(markdown_text)
    doc = SimpleDocTemplate(
        str(PDF_PATH),
        pagesize=A4,
        rightMargin=0.65 * inch,
        leftMargin=0.65 * inch,
        topMargin=0.7 * inch,
        bottomMargin=0.7 * inch,
        title="Android/Mobile Cryptography Benchmark Analysis Report",
        author="GitHub Copilot",
    )
    doc.build(story)
    print(f"Created PDF: {PDF_PATH}")


if __name__ == "__main__":
    main()
