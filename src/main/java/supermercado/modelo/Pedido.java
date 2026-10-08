package supermercado.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.excecao.RegraNegocioException;

/**
 * Pedido fechado por um cliente.
 *
 * <p>O pedido guarda uma cópia imutável dos itens no momento do fechamento.
 * Antes ele guardava uma referência ao carrinho, então qualquer item
 * adicionado ao carrinho depois do fechamento alterava o total do pedido.</p>
 *
 * <p>Esta classe contém apenas dados e regras do pedido. Ela não monta textos
 * nem sabe onde é gravada: isso é papel de outras camadas.</p>
 */
public class Pedido {

    private final int numero;
    private final Cliente cliente;
    private final List<ItemCarrinho> itens;
    private final LocalDateTime dataHora;
    private final BigDecimal subtotal;
    private final BigDecimal desconto;
    private final String descricaoDesconto;
    private StatusPedido status;

    /**
     * Fecha um pedido novo agora, aplicando a política de desconto.
     *
     * @param politicaDeDesconto estratégia de desconto aplicada a este pedido
     *                           (use {@code new SemDesconto()} quando não houver)
     */
    public Pedido(int numero, Cliente cliente, List<ItemCarrinho> itens,
                  PoliticaDeDesconto politicaDeDesconto) {
        this(numero, cliente, itens, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                Objects.requireNonNull(politicaDeDesconto, "A política de desconto é obrigatória.")
                        .calcularDesconto(somarItens(validarItens(itens))),
                politicaDeDesconto.getDescricao(), StatusPedido.FECHADO);
    }

    private Pedido(int numero, Cliente cliente, List<ItemCarrinho> itens, LocalDateTime dataHora,
                   BigDecimal desconto, String descricaoDesconto, StatusPedido status) {
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do pedido deve ser maior que zero.");
        }
        this.numero = numero;
        this.cliente = Objects.requireNonNull(cliente, "O cliente é obrigatório.");
        this.itens = List.copyOf(validarItens(itens));
        this.dataHora = Objects.requireNonNull(dataHora, "A data do pedido é obrigatória.");
        this.subtotal = somarItens(this.itens);
        this.desconto = validarDesconto(desconto);
        this.descricaoDesconto = descricaoDesconto == null ? "" : descricaoDesconto;
        this.status = Objects.requireNonNull(status, "A situação do pedido é obrigatória.");
    }

    /**
     * Recria um pedido que já foi fechado e gravado (por exemplo, lido do banco
     * de dados). O desconto não é recalculado: vale o que foi gravado no dia.
     */
    public static Pedido restaurar(int numero, Cliente cliente, List<ItemCarrinho> itens,
                                   LocalDateTime dataHora, BigDecimal desconto,
                                   String descricaoDesconto, StatusPedido status) {
        return new Pedido(numero, cliente, itens, dataHora, desconto, descricaoDesconto, status);
    }

    private static List<ItemCarrinho> validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("Um pedido precisa ter pelo menos um item.");
        }
        return itens;
    }

    private static BigDecimal somarItens(List<ItemCarrinho> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            soma = soma.add(item.calcularSubtotal());
        }
        return soma;
    }

    /** Garante o contrato de {@link PoliticaDeDesconto}: 0 <= desconto <= subtotal. */
    private BigDecimal validarDesconto(BigDecimal valorDesconto) {
        if (valorDesconto == null || valorDesconto.signum() < 0 || valorDesconto.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("Desconto inválido: deve ficar entre zero e o subtotal do pedido.");
        }
        return valorDesconto.setScale(2, RoundingMode.HALF_UP);
    }

    /** Regra de negócio: um pedido cancelado não pode ser cancelado de novo. */
    public void cancelar() {
        if (status == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido nº " + numero + " já está cancelado.");
        }
        status = StatusPedido.CANCELADO;
    }

    public int getNumero() {
        return numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    /** Lista somente leitura com os itens do pedido. */
    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public String getDescricaoDesconto() {
        return descricaoDesconto;
    }

    /** Total a pagar: subtotal menos o desconto. */
    public BigDecimal getTotal() {
        return subtotal.subtract(desconto);
    }

    /** Soma das quantidades de todos os itens. */
    public int quantidadeDeUnidades() {
        int soma = 0;
        for (ItemCarrinho item : itens) {
            soma += item.getQuantidade();
        }
        return soma;
    }
}
