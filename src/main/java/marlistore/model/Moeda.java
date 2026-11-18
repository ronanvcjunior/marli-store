package marlistore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formatação de valores monetários no padrão brasileiro: {@code R$ 1.234,50} (RNF-09). */
public final class Moeda {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Moeda() {
    }

    /**
     * Formata um valor em reais.
     *
     * @param valor valor monetário (nulo é tratado como zero)
     * @return texto como {@code R$ 0,00}
     */
    public static String formatar(BigDecimal valor) {
        BigDecimal v = (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
        return "R$ " + new DecimalFormat("#,##0.00", new DecimalFormatSymbols(PT_BR)).format(v);
    }
}
