package org.vc.service;

import org.vc.pdf.XmlPaymentHtmlPdfWriter;
import org.vc.xml.XmlPaymentDocumentParser;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Координирует генерацию PDF-платёжек из XML.
 */
public class XmlPaymentPdfGenerationService {

    private final XmlPaymentDocumentParser parser = new XmlPaymentDocumentParser();

    /**
     * Создаёт PDF-файлы из XML, разбивая результат на части.
     */
    public int generate(Path xmlFile, Path outputFolder, int maxDocumentsPerFile) throws IOException, XMLStreamException {
        try (XmlPaymentHtmlPdfWriter writer = new XmlPaymentHtmlPdfWriter(outputFolder, maxDocumentsPerFile)) {
            int parsedDocuments = parser.parse(xmlFile, writer::write);

            System.out.println("Прочитано платёжек из XML: " + parsedDocuments);
            System.out.println("Записано платёжек в PDF: " + writer.getWrittenDocuments());

            return parsedDocuments;
        }
    }

    /**
     * Создаёт PDF-файлы для всех XML из выбранной папки.
     */
    public int generateFromFolder(Path xmlFolder, Path outputRoot, int maxDocumentsPerFile) throws IOException, XMLStreamException {
        List<Path> xmlFiles = findXmlFiles(xmlFolder);
        int totalDocuments = 0;

        if (xmlFiles.isEmpty()) {
            throw new IllegalStateException("В выбранной папке нет XML-файлов: " + xmlFolder);
        }

        Files.createDirectories(outputRoot);

        System.out.println("Найдено XML-файлов: " + xmlFiles.size());

        for (Path xmlFile : xmlFiles) {
            Path xmlOutputFolder = outputRoot.resolve(removeExtension(xmlFile.getFileName().toString()));

            System.out.println("Обработка XML: " + xmlFile);
            System.out.println("Папка PDF: " + xmlOutputFolder);

            totalDocuments += generate(xmlFile, xmlOutputFolder, maxDocumentsPerFile);
        }

        System.out.println("Всего сформировано платёжек из XML: " + totalDocuments);

        return totalDocuments;
    }

    private List<Path> findXmlFiles(Path xmlFolder) throws IOException {
        try (Stream<Path> paths = Files.walk(xmlFolder)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(this::isXml)
                .toList();
        }
    }

    private boolean isXml(Path file) {
        return file.getFileName()
            .toString()
            .toLowerCase(Locale.ROOT)
            .endsWith(".xml");
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }
}
