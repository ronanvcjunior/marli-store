package marlistore.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import marlistore.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;

class ValidacaoTest {

    @Test
    void cpfValidoAceitaComEsemPontuacao() {
        assertTrue(Validacao.cpfValido("529.982.247-25"));
        assertTrue(Validacao.cpfValido("52998224725"));
    }

    @Test
    void cpfInvalidoRejeitaDigitoErradoRepetidosETamanho() {
        assertFalse(Validacao.cpfValido("529.982.247-24"));
        assertFalse(Validacao.cpfValido("111.111.111-11"));
        assertFalse(Validacao.cpfValido("123"));
        assertFalse(Validacao.cpfValido(null));
    }

    @Test
    void dataExigeFormatoEDiaReal() throws Exception {
        assertEquals(LocalDate.of(2026, 11, 6), Validacao.data("06/11/2026"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.data("31/02/2026"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.data("2026-11-06"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.data(""));
    }

    @Test
    void decimalAceitaFormatoBrasileiro() throws Exception {
        assertEquals(new BigDecimal("12.50"), Validacao.decimal("12,50", "preço"));
        assertEquals(new BigDecimal("1234.50"), Validacao.decimal("R$ 1.234,50", "preço"));
        assertEquals(new BigDecimal("12.5"), Validacao.decimal("12.5", "preço"));
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class, () -> Validacao.decimal("abc", "preço"));
        assertTrue(e.getMessage().contains("preço"));
    }

    @Test
    void numerosPositivos() {
        assertThrows(DadosInvalidosException.class, () -> Validacao.inteiroPositivo(0, "quantidade"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.inteiroNaoNegativo(-1, "estoque"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.valorPositivo(BigDecimal.ZERO, "preço"));
        assertThrows(DadosInvalidosException.class, () -> Validacao.texto("  ", "nome"));
    }

    @Test
    void moedaFormataNoPadraoBrasileiro() {
        assertEquals("R$ 0,00", Moeda.formatar(BigDecimal.ZERO));
        assertEquals("R$ 1.234,50", Moeda.formatar(new BigDecimal("1234.5")));
    }
}
