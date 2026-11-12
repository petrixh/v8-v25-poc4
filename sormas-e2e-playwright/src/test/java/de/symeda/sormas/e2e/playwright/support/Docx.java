package de.symeda.sormas.e2e.playwright.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Word documents the app generates. */
public final class Docx {

	private Docx() {
	}

	/** The paragraph texts of a .docx (word/document.xml), one per paragraph, empty ones left out. */
	public static List<String> paragraphs(Path file) throws IOException {
		String xml;
		try (ZipFile z = new ZipFile(file.toFile())) {
			xml = new String(z.getInputStream(z.getEntry("word/document.xml")).readAllBytes(), StandardCharsets.UTF_8);
		}
		List<String> out = new ArrayList<>();
		Matcher p = Pattern.compile("<w:p[ >].*?</w:p>", Pattern.DOTALL).matcher(xml);
		while (p.find()) {
			StringBuilder sb = new StringBuilder();
			Matcher t = Pattern.compile("<w:t[^>]*>([^<]*)</w:t>").matcher(p.group());
			while (t.find()) {
				sb.append(t.group(1));
			}
			String s = sb.toString().trim();
			if (!s.isEmpty()) {
				out.add(s);
			}
		}
		return out;
	}

	/** The sheet names of an .xlsx workbook (xl/workbook.xml), in order. */
	public static List<String> sheetNames(Path file) throws IOException {
		String xml;
		try (ZipFile z = new ZipFile(file.toFile())) {
			xml = new String(z.getInputStream(z.getEntry("xl/workbook.xml")).readAllBytes(), StandardCharsets.UTF_8);
		}
		List<String> out = new ArrayList<>();
		Matcher m = Pattern.compile("<sheet [^>]*name=\"([^\"]+)\"").matcher(xml);
		while (m.find()) {
			out.add(m.group(1));
		}
		return out;
	}

	/** The strings of an .xlsx workbook's shared string table, one per entry, white space collapsed. */
	public static List<String> sharedStrings(Path file) throws IOException {
		String xml;
		try (ZipFile z = new ZipFile(file.toFile())) {
			xml = new String(z.getInputStream(z.getEntry("xl/sharedStrings.xml")).readAllBytes(), StandardCharsets.UTF_8);
		}
		List<String> out = new ArrayList<>();
		Matcher si = Pattern.compile("<si>(.*?)</si>", Pattern.DOTALL).matcher(xml);
		while (si.find()) {
			StringBuilder sb = new StringBuilder();
			Matcher t = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL).matcher(si.group(1));
			while (t.find()) {
				sb.append(t.group(1));
			}
			String s = sb.toString().replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replaceAll("\\s+", " ").trim();
			if (!s.isEmpty()) {
				out.add(s);
			}
		}
		return out;
	}
}
