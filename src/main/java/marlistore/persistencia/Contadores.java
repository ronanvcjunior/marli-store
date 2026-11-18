package marlistore.persistencia;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import marlistore.model.Categoria;
import marlistore.model.Cliente;
import marlistore.model.Pedido;
import marlistore.model.Produto;
import marlistore.model.Promocao;

/**
 * Grava e carrega os contadores de ID de cada classe em {@code contadores.dat}.
 * Recalcular os contadores pelo maior ID existente reaproveitaria números após
 * uma exclusão e violaria a RN-02; por isso eles são gravados à parte.
 */
public final class Contadores {

    private Contadores() {
    }

    /**
     * Carrega os contadores gravados. Um contador nunca recua; arquivo ausente não altera nada.
     *
     * @param arquivo arquivo {@code contadores.dat}
     */
    public static void carregar(Path arquivo) {
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String[] kv = linha.split("=");
                if (kv.length != 2) {
                    continue;
                }
                try {
                    int v = Integer.parseInt(kv[1].trim());
                    switch (kv[0].trim()) {
                        case "Categoria": Categoria.setProximoId(v); break;
                        case "Produto": Produto.setProximoId(v); break;
                        case "Cliente": Cliente.setProximoId(v); break;
                        case "Pedido": Pedido.setProximoId(v); break;
                        case "Promocao": Promocao.setProximoId(v); break;
                        default: break;
                    }
                } catch (NumberFormatException e) {
                    // linha ilegível: ignorada, o contador é protegido pelo maior ID existente
                }
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Não foi possível ler contadores.dat", e);
        }
    }

    /**
     * Grava os contadores atuais de forma segura.
     *
     * @param arquivo arquivo {@code contadores.dat}
     */
    public static void salvar(Path arquivo) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Categoria=" + Categoria.getProximoId());
        linhas.add("Produto=" + Produto.getProximoId());
        linhas.add("Cliente=" + Cliente.getProximoId());
        linhas.add("Pedido=" + Pedido.getProximoId());
        linhas.add("Promocao=" + Promocao.getProximoId());
        Repositorio.gravarSeguro(arquivo, linhas);
    }
}
