package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.Optional;
import java.util.UUID;

public class Peca {

    private static final String CODIGO = "PECA_INVALIDA";
    private static final String UNIDADE_PADRAO = "unidade";
    private static final int SALDO_MAXIMO = Integer.MAX_VALUE;

    private final UUID id;
    private String nome;
    private String unidadeMedida;
    private Dinheiro preco;
    private int saldoEmEstoque;
    private int quantidadeReservada;
    private int estoqueMinimo;
    private boolean ativo;

    public Peca(UUID id, String nome, String unidadeMedida, Dinheiro preco,
                int saldoEmEstoque, int quantidadeReservada, int estoqueMinimo, boolean ativo) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Peca exige identidade");
        }
        this.id = id;
        this.ativo = ativo;
        this.saldoEmEstoque = saldoEmEstoque;
        this.quantidadeReservada = quantidadeReservada;
        alterar(nome, unidadeMedida, preco, estoqueMinimo);
        garantirInvariantes();
    }

    public static Peca cadastrar(String nome, String unidadeMedida, Dinheiro preco, int estoqueMinimo) {
        return new Peca(UUID.randomUUID(), nome, unidadeMedida, preco, 0, 0, estoqueMinimo, true);
    }

    public final void alterar(String nome, String unidadeMedida, Dinheiro preco, int estoqueMinimo) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException(CODIGO, "Peca exige nome");
        }
        if (preco == null) {
            throw new DadosInvalidosException(CODIGO, "Peca exige preco");
        }
        if (estoqueMinimo < 0) {
            throw new DadosInvalidosException(CODIGO, "Estoque minimo nao pode ser negativo");
        }
        this.nome = nome.trim();
        this.unidadeMedida = unidadeDeMedidaOuPadrao(unidadeMedida);
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void inativar() {
        this.ativo = false;
    }

    public void registrarEntrada(int quantidade) {
        if (quantidade <= 0) {
            throw new DadosInvalidosException(CODIGO, "Entrada de estoque exige quantidade positiva");
        }
        somarAoSaldo(quantidade);
    }

    public ResultadoDaReserva reservar(UUID ordemServicoId, int quantidade) {
        if (quantidade <= 0) {
            throw new DadosInvalidosException(CODIGO, "Reserva de peca exige quantidade positiva");
        }
        int aReservar = Math.min(saldoDisponivel(), quantidade);
        return new ResultadoDaReserva(
                separar(ordemServicoId, aReservar),
                registrarFaltaDaReserva(ordemServicoId, quantidade - aReservar));
    }

    public PendenciaPeca registrarFalta(UUID ordemServicoId, int quantidadeFaltante) {
        if (quantidadeFaltante <= 0) {
            throw new DadosInvalidosException(CODIGO, "Falta de peca exige quantidade faltante positiva");
        }
        return PendenciaPeca.registrar(ordemServicoId, this, quantidadeFaltante);
    }

    public void retirar(ReservaPeca reserva) {
        garantirQueAReservaEDestaPeca(reserva);
        reserva.consumir();
        this.saldoEmEstoque -= reserva.quantidade();
        this.quantidadeReservada -= reserva.quantidade();
        garantirInvariantes();
    }

    public void devolver(ReservaPeca reserva) {
        garantirQueAReservaEDestaPeca(reserva);
        boolean reporSaldo = reserva.estaConsumida();
        reserva.devolver();
        if (reporSaldo) {
            somarAoSaldo(reserva.quantidade());
            return;
        }
        this.quantidadeReservada -= reserva.quantidade();
        garantirInvariantes();
    }

    public int saldoDisponivel() {
        return saldoEmEstoque - quantidadeReservada;
    }

    public boolean abaixoDoEstoqueMinimo() {
        return saldoEmEstoque < estoqueMinimo;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String unidadeMedida() {
        return unidadeMedida;
    }

    public Dinheiro preco() {
        return preco;
    }

    public int saldoEmEstoque() {
        return saldoEmEstoque;
    }

    public int quantidadeReservada() {
        return quantidadeReservada;
    }

    public int estoqueMinimo() {
        return estoqueMinimo;
    }

    public boolean ativo() {
        return ativo;
    }

    private Optional<ReservaPeca> separar(UUID ordemServicoId, int quantidade) {
        if (quantidade <= 0) {
            return Optional.empty();
        }
        this.quantidadeReservada += quantidade;
        garantirInvariantes();
        return Optional.of(ReservaPeca.ativar(ordemServicoId, this, quantidade));
    }

    private Optional<PendenciaPeca> registrarFaltaDaReserva(UUID ordemServicoId, int quantidadeFaltante) {
        if (quantidadeFaltante <= 0) {
            return Optional.empty();
        }
        return Optional.of(registrarFalta(ordemServicoId, quantidadeFaltante));
    }

    private void somarAoSaldo(int quantidade) {
        if (quantidade > SALDO_MAXIMO - saldoEmEstoque) {
            throw new DadosInvalidosException(CODIGO, "Saldo em estoque excede o teto de " + SALDO_MAXIMO);
        }
        this.saldoEmEstoque += quantidade;
        garantirInvariantes();
    }

    private void garantirQueAReservaEDestaPeca(ReservaPeca reserva) {
        if (reserva == null || !id.equals(reserva.pecaId())) {
            throw new DadosInvalidosException(CODIGO, "Reserva de peca nao pertence a esta Peca");
        }
    }

    private void garantirInvariantes() {
        if (saldoEmEstoque < 0) {
            throw new SaldoEmEstoqueNegativoException(id);
        }
        if (quantidadeReservada < 0) {
            throw new QuantidadeReservadaInvalidaException(id, "nao pode ser negativa");
        }
        if (quantidadeReservada > saldoEmEstoque) {
            throw new QuantidadeReservadaInvalidaException(id, "nao pode exceder o Saldo em estoque");
        }
    }

    private String unidadeDeMedidaOuPadrao(String unidadeMedida) {
        if (unidadeMedida == null || unidadeMedida.isBlank()) {
            return UNIDADE_PADRAO;
        }
        return unidadeMedida.trim();
    }
}
