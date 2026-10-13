package com.transport.reporting.service;

import com.transport.reporting.dto.ReportAnalysisDetail;
import com.transport.reporting.dto.ReportAnalysisResponse;
import com.transport.reporting.dto.ReportAnalysisRow;
import com.transport.reporting.exception.BusinessException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** PDF et Excel produits par JasperReports à partir du même résultat d'analyse. */
@Service
public class ReportAnalysisExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportAnalysisExportService.class);
    private final Map<String, JasperReport> compiled = new ConcurrentHashMap<>();

    public byte[] pdf(ReportAnalysisResponse analysis) {
        try (InputStream logo = logo()) {
            Map<String, Object> parameters = parameters(analysis, "");
            parameters.put("LOGO", logo);
            JasperPrint print = JasperFillManager.fillReport(
                    report("reports/analysis-pdf.jrxml"),
                    parameters,
                    source(analysis.getRows()));
            return JasperExportManager.exportReportToPdf(print);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Échec de génération du PDF d'analyse", ex);
            throw new BusinessException("Le rapport n'a pas pu être généré.", "REPORT_GENERATION_FAILED");
        }
    }

    public byte[] excel(ReportAnalysisResponse analysis, List<ReportAnalysisDetail> details) {
        try {
            String note = analysis.isDetailTruncated()
                    ? "Le détail est limité aux " + analysis.getDetailLimit() + " signalements les plus récents."
                    : "";
            List<JasperPrint> prints = new ArrayList<>();
            List<String> sheets = new ArrayList<>();
            prints.add(JasperFillManager.fillReport(
                    report("reports/analysis-excel-summary.jrxml"),
                    parameters(analysis, note),
                    new JREmptyDataSource()));
            sheets.add("Synthèse");
            prints.add(JasperFillManager.fillReport(
                    report("reports/analysis-excel-groups.jrxml"),
                    new HashMap<>(),
                    source(analysis.getRows())));
            sheets.add("Résultats regroupés");
            if (details != null && !details.isEmpty()) {
                prints.add(JasperFillManager.fillReport(
                        report("reports/analysis-excel-detail.jrxml"),
                        new HashMap<>(),
                        new JRBeanCollectionDataSource(details)));
                sheets.add("Détail des signalements");
            }
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setRemoveEmptySpaceBetweenColumns(true);
            configuration.setWhitePageBackground(false);
            configuration.setIgnoreGraphics(true);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(sheets.toArray(String[]::new));
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Échec de génération de l'Excel d'analyse", ex);
            throw new BusinessException("Le rapport n'a pas pu être généré.", "REPORT_GENERATION_FAILED");
        }
    }

    private Map<String, Object> parameters(ReportAnalysisResponse analysis, String detailNote) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("TITLE", analysis.getTitle());
        parameters.put("SUBTITLE", analysis.getSubtitle());
        parameters.put("PERIOD", analysis.getPeriodLabel());
        parameters.put("REPORT_TYPE", analysis.getReportTypeLabel());
        parameters.put("SUPPORT_TYPE", analysis.getSupportTypeLabel());
        parameters.put("SUPPORT", analysis.getSupportLabel());
        parameters.put("GROUPING", analysis.getGroupingLabel());
        parameters.put("TOTAL", analysis.getTotalReports());
        parameters.put("GROUP_COUNT", analysis.getGroupCount());
        parameters.put("GENERATED_AT", analysis.getGeneratedAt());
        parameters.put("DETAIL_NOTE", detailNote);
        return parameters;
    }

    private JasperReport report(String classpath) {
        return compiled.computeIfAbsent(classpath, path -> {
            try (InputStream input = new ClassPathResource(path).getInputStream()) {
                return JasperCompileManager.compileReport(input);
            } catch (Exception ex) {
                throw new IllegalStateException("Modèle de rapport illisible : " + path, ex);
            }
        });
    }

    private static JRDataSource source(List<ReportAnalysisRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return new JREmptyDataSource();
        }
        return new JRBeanCollectionDataSource(rows);
    }

    private static InputStream logo() {
        try {
            ClassPathResource resource = new ClassPathResource("email/transtu_logo.png");
            return resource.exists() ? resource.getInputStream() : InputStream.nullInputStream();
        } catch (Exception ex) {
            return InputStream.nullInputStream();
        }
    }
}
