package supermercado.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Produto do catálogo do supermercado.
 *
 * <p>Na etapa de refatoração a quantidade comprada saiu desta classe e foi
 * para {@link ItemCarrinho}: o produto representa só o item de catálogo.</p>
 *
 * <p>Na versão web o catálogo vem do banco de dados e a vitrine precisa de
 * mais três informações descritivas: embalagem ({@code unidade}),
 * categoria e um ícone. Elas são opcionais, então o construtor antigo
 * (código, nome e preço) continua valendo para os testes e para o DAO em
 * memória.</p>
 */
public class Produto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int codigo;
    private final String nome;
    private final BigDecimal precoUnitario;
    private final String unidade;
    private final String categoria;
    private final String icone;

    public Produto(int codigo, String nome, BigDecimal precoUnitario) {
        this(codigo, nome, precoUnitario, "", "", "");
    }

    public Produto(int codigo, String nome, BigDecimal precoUnitario,
                   String unidade, String categoria, String icone) {
        if (codigo <= 0) {
            throw new IllegalArgumentException("O código do produto deve ser maior que zero.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new IllegalArgumentException("O preço unitário não pode ser negativo.");
        }
        this.codigo = codigo;
        this.nome = nome.trim();
        this.precoUnitario = precoUnitario.setScale(2, RoundingMode.HALF_UP);
        this.unidade = textoOuVazio(unidade);
        this.categoria = textoOuVazio(categoria);
        this.icone = textoOuVazio(icone);
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    /** Embalagem ou forma de venda, ex.: "Pacote 1 kg". */
    public String getUnidade() {
        return unidade;
    }

    public String getCategoria() {
        return categoria;
    }

    /** Ícone (emoji) mostrado na vitrine enquanto não há fotos dos produtos. */
    public String getIcone() {
        return icone;
    }

    /** Dois produtos são o mesmo item de catálogo quando têm o mesmo código. */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Produto)) {
            return false;
        }
        return codigo == ((Produto) outro).codigo;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(codigo);
    }

    @Override
    public String toString() {
        return nome + " (cód. " + codigo + ")";
    }
}
