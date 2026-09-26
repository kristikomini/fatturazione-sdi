package it.kristikomini.fatturapa.sdi;

/**
 * One error from a {@code NotificaScarto}. SDI identifies each rejection reason by a numeric
 * {@code codice}; {@link ScartoCode} is a small catalog of the common ones with their meaning and
 * whether the fix is "correct and resend" or "manual review".
 */
public record SdiError(String codice, String descrizione) {

    public static SdiError of(ScartoCode code) {
        return new SdiError(code.code(), code.description());
    }

    /** A representative subset of the real SDI rejection codes. */
    public enum ScartoCode {
        NOME_FILE_NON_VALIDO("00001", "Nome file non valido", Resolution.CORRECT_AND_RESEND),
        NOME_FILE_DUPLICATO("00003", "Nome file duplicato", Resolution.CORRECT_AND_RESEND),
        FORMATO_NON_CONFORME("00200", "File non conforme al formato", Resolution.CORRECT_AND_RESEND),
        FATTURA_DUPLICATA("00404", "Fattura duplicata", Resolution.MANUAL_REVIEW),
        CODICE_DESTINATARIO_NON_VALIDO("00427", "Codice destinatario non valido", Resolution.CORRECT_AND_RESEND);

        private final String code;
        private final String description;
        private final Resolution resolution;

        ScartoCode(String code, String description, Resolution resolution) {
            this.code = code;
            this.description = description;
            this.resolution = resolution;
        }

        public String code() {
            return code;
        }

        public String description() {
            return description;
        }

        public Resolution resolution() {
            return resolution;
        }
    }

    public enum Resolution {
        CORRECT_AND_RESEND,
        MANUAL_REVIEW
    }
}
