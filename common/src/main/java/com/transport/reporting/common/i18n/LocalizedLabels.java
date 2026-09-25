package com.transport.reporting.common.i18n;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.entity.ReportNature;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.SupportType;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Résolution des libellés FR / AR / EN selon {@link LocaleContextHolder}
 * (alimenté par {@code Accept-Language}).
 *
 * <p>Fallback : langue demandée → français → première valeur non vide.</p>
 */
public final class LocalizedLabels {

    private LocalizedLabels() {
    }

    public static Locale currentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null) {
            return Locale.FRENCH;
        }
        return locale;
    }

    /** Normalise un tag Accept-Language en fr | ar | en (défaut fr). */
    public static Locale fromAcceptLanguage(String acceptLanguage) {
        if (!StringUtils.hasText(acceptLanguage)) {
            return Locale.FRENCH;
        }
        String primary = acceptLanguage.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (primary.startsWith("ar")) {
            return Locale.forLanguageTag("ar");
        }
        if (primary.startsWith("en")) {
            return Locale.ENGLISH;
        }
        return Locale.FRENCH;
    }

    public static String resolve(String labelFr, String labelAr, String labelEn) {
        return resolve(labelFr, labelAr, labelEn, currentLocale());
    }

    public static String resolve(String labelFr, String labelAr, String labelEn, Locale locale) {
        String lang = locale != null ? locale.getLanguage() : "fr";
        String preferred = switch (lang) {
            case "ar" -> labelAr;
            case "en" -> labelEn;
            default -> labelFr;
        };
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        if (StringUtils.hasText(labelFr)) {
            return labelFr.trim();
        }
        if (StringUtils.hasText(labelEn)) {
            return labelEn.trim();
        }
        if (StringUtils.hasText(labelAr)) {
            return labelAr.trim();
        }
        return null;
    }

    /** FR effectif : label_fr prioritaire, sinon miroir historique {@code label}. */
    public static String frOf(String labelFr, String legacyLabel) {
        if (StringUtils.hasText(labelFr)) {
            return labelFr.trim();
        }
        return StringUtils.hasText(legacyLabel) ? legacyLabel.trim() : null;
    }

    public static String of(SupportType entity) {
        if (entity == null) {
            return null;
        }
        return resolve(frOf(entity.getLabelFr(), entity.getLabel()), entity.getLabelAr(), entity.getLabelEn());
    }

    public static String of(ReportType entity) {
        if (entity == null) {
            return null;
        }
        return resolve(frOf(entity.getLabelFr(), entity.getLabel()), entity.getLabelAr(), entity.getLabelEn());
    }

    public static String of(ReportNature entity) {
        if (entity == null) {
            return null;
        }
        return resolve(frOf(entity.getLabelFr(), entity.getLabel()), entity.getLabelAr(), entity.getLabelEn());
    }

    public static String of(Status entity) {
        if (entity == null) {
            return null;
        }
        return resolve(frOf(entity.getLabelFr(), entity.getLabel()), entity.getLabelAr(), entity.getLabelEn());
    }

    public static String priority(Priority priority) {
        return priority(priority, currentLocale());
    }

    public static String priority(Priority priority, Locale locale) {
        if (priority == null) {
            return null;
        }
        String lang = locale != null ? locale.getLanguage() : "fr";
        return switch (priority) {
            case LOW -> switch (lang) {
                case "ar" -> "منخفضة";
                case "en" -> "Low";
                default -> "Faible";
            };
            case MEDIUM -> switch (lang) {
                case "ar" -> "عادية";
                case "en" -> "Normal";
                default -> "Normale";
            };
            case HIGH -> switch (lang) {
                case "ar" -> "مرتفعة";
                case "en" -> "High";
                default -> "Élevée";
            };
            case CRITICAL -> switch (lang) {
                case "ar" -> "حرجة";
                case "en" -> "Critical";
                default -> "Critique";
            };
        };
    }

    /** Applique le libellé FR et synchronise le miroir historique {@code label}. */
    public static void applyFrench(SupportType entity, String labelFr) {
        String fr = requireFr(labelFr);
        entity.setLabelFr(fr);
        entity.setLabel(fr);
    }

    public static void applyFrench(ReportType entity, String labelFr) {
        String fr = requireFr(labelFr);
        entity.setLabelFr(fr);
        entity.setLabel(fr);
    }

    public static void applyFrench(ReportNature entity, String labelFr) {
        String fr = requireFr(labelFr);
        entity.setLabelFr(fr);
        entity.setLabel(fr);
    }

    public static void applyFrench(Status entity, String labelFr) {
        String fr = requireFr(labelFr);
        entity.setLabelFr(fr);
        entity.setLabel(fr);
    }

    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String requireFr(String labelFr) {
        if (!StringUtils.hasText(labelFr)) {
            throw new IllegalArgumentException("Le libellé français est obligatoire");
        }
        return labelFr.trim();
    }
}
