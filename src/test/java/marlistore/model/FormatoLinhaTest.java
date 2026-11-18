package marlistore.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FormatoLinhaTest {

    @Test
    void idaEVoltaPreservaCaracteresEspeciais() {
        String a = "ponto;e;virgula";
        String b = "barra\\invertida";
        String c = "linha1\nlinha2";
        String linha = FormatoLinha.juntar(a, "", b, c);
        assertEquals(List.of(a, "", b, c), FormatoLinha.dividir(linha));
    }

    @Test
    void camposVaziosEnulos() {
        assertEquals(List.of("", "x", ""), FormatoLinha.dividir(FormatoLinha.juntar(null, "x", null)));
    }

    @Test
    void separadorAlternativoAninhado() {
        String interno = FormatoLinha.juntar(',', "a,b", "c;d");
        String externo = FormatoLinha.juntar("1", interno);
        List<String> campos = FormatoLinha.dividir(externo);
        assertEquals(List.of("a,b", "c;d"), FormatoLinha.dividir(',', campos.get(1)));
    }
}
