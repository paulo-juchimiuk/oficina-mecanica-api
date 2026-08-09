package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Peca")
class PecaTest {

    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("189.90"), "BRL");

    private Peca comSaldo(int saldo, int reservada) {
        return new Peca(UUID.randomUUID(), "Pastilha de freio dianteira", "unidade", PRECO,
                saldo, reservada, 4, true);
    }

    @Test
    @DisplayName("deve cadastrar peca ativa com Saldo em estoque zerado")
    void deveCadastrarComSaldoZerado() {
        Peca peca = Peca.cadastrar("Filtro de oleo", "unidade", PRECO, 10);

        assertThat(peca.saldoEmEstoque()).isZero();
        assertThat(peca.quantidadeReservada()).isZero();
        assertThat(peca.ativo()).isTrue();
        assertThat(peca.estoqueMinimo()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("deve aplicar a unidade de medida padrao quando ela nao vem informada")
    void deveAplicarUnidadePadrao(String unidadeMedida) {
        assertThat(Peca.cadastrar("Filtro de oleo", unidadeMedida, PRECO, 10).unidadeMedida())
                .isEqualTo("unidade");
    }

    @Test
    @DisplayName("deve aplicar a unidade de medida padrao quando ela vem ausente")
    void deveAplicarUnidadePadraoQuandoAusente() {
        assertThat(Peca.cadastrar("Filtro de oleo", null, PRECO, 10).unidadeMedida()).isEqualTo("unidade");
    }

    @Test
    @DisplayName("deve preservar a unidade de medida do Insumo")
    void devePreservarUnidadeDoInsumo() {
        assertThat(Peca.cadastrar("Oleo lubrificante 5W30", "litro", PRECO, 20).unidadeMedida())
                .isEqualTo("litro");
    }

    @Test
    @DisplayName("deve recusar peca sem nome")
    void deveRecusarSemNome() {
        assertThatThrownBy(() -> Peca.cadastrar("   ", "unidade", PRECO, 10))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar peca sem preco")
    void deveRecusarSemPreco() {
        assertThatThrownBy(() -> Peca.cadastrar("Filtro de oleo", "unidade", null, 10))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar Estoque minimo negativo")
    void deveRecusarEstoqueMinimoNegativo() {
        assertThatThrownBy(() -> Peca.cadastrar("Filtro de oleo", "unidade", PRECO, -1))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar peca sem identidade")
    void deveRecusarSemIdentidade() {
        assertThatThrownBy(() -> new Peca(null, "Filtro de oleo", "unidade", PRECO, 0, 0, 0, true))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve alterar preservando a identidade e o saldo")
    void deveAlterarPreservandoIdentidadeESaldo() {
        Peca peca = comSaldo(24, 2);
        UUID identidade = peca.id();

        peca.alterar("Filtro de oleo", "unidade", new Dinheiro(new BigDecimal("35.90"), "BRL"), 8);

        assertThat(peca.id()).isEqualTo(identidade);
        assertThat(peca.nome()).isEqualTo("Filtro de oleo");
        assertThat(peca.estoqueMinimo()).isEqualTo(8);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
        assertThat(peca.quantidadeReservada()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve inativar sem tocar no saldo")
    void deveInativar() {
        Peca peca = comSaldo(24, 0);

        peca.inativar();

        assertThat(peca.ativo()).isFalse();
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
    }

    @Test
    @DisplayName("deve somar a Entrada de estoque ao Saldo em estoque")
    void deveRegistrarEntrada() {
        Peca peca = comSaldo(2, 0);

        peca.registrarEntrada(10);

        assertThat(peca.saldoEmEstoque()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("deve recusar Entrada de estoque sem quantidade positiva")
    void deveRecusarEntradaNaoPositiva(int quantidade) {
        assertThatThrownBy(() -> comSaldo(2, 0).registrarEntrada(quantidade))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar Entrada de estoque que estoura o teto da coluna de saldo")
    void deveRecusarEntradaAcimaDoTeto() {
        Peca peca = comSaldo(Integer.MAX_VALUE - 1, 0);

        assertThatThrownBy(() -> peca.registrarEntrada(2))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve reservar a quantidade inteira quando ha Saldo em estoque disponivel")
    void deveReservarQuantidadeInteira() {
        Peca peca = comSaldo(24, 0);
        UUID ordemServicoId = UUID.randomUUID();

        ResultadoDaReserva resultado = peca.reservar(ordemServicoId, 4);

        assertThat(resultado.pendencia()).isEmpty();
        assertThat(resultado.reserva()).isPresent();
        assertThat(resultado.reserva().orElseThrow().quantidade()).isEqualTo(4);
        assertThat(resultado.reserva().orElseThrow().estaAtiva()).isTrue();
        assertThat(resultado.reserva().orElseThrow().ordemServicoId()).isEqualTo(ordemServicoId);
        assertThat(resultado.reserva().orElseThrow().nomePeca()).isEqualTo(peca.nome());
        assertThat(peca.quantidadeReservada()).isEqualTo(4);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
    }

    @Test
    @DisplayName("deve reservar o disponivel e transformar em pendencia o que faltou")
    void deveReservarParcialmenteEGerarPendencia() {
        Peca peca = comSaldo(1, 0);

        ResultadoDaReserva resultado = peca.reservar(UUID.randomUUID(), 2);

        assertThat(resultado.reserva().orElseThrow().quantidade()).isEqualTo(1);
        assertThat(resultado.pendencia().orElseThrow().quantidadeFaltante()).isEqualTo(1);
        assertThat(peca.quantidadeReservada()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve gerar apenas pendencia quando nao ha Saldo em estoque disponivel")
    void deveGerarApenasPendencia() {
        Peca peca = comSaldo(3, 3);

        ResultadoDaReserva resultado = peca.reservar(UUID.randomUUID(), 2);

        assertThat(resultado.reserva()).isEmpty();
        assertThat(resultado.pendencia().orElseThrow().quantidadeFaltante()).isEqualTo(2);
        assertThat(peca.quantidadeReservada()).isEqualTo(3);
    }

    @Test
    @DisplayName("deve descontar as reservas ja existentes ao calcular o disponivel")
    void deveDescontarReservasExistentes() {
        Peca peca = comSaldo(5, 3);

        ResultadoDaReserva resultado = peca.reservar(UUID.randomUUID(), 4);

        assertThat(resultado.reserva().orElseThrow().quantidade()).isEqualTo(2);
        assertThat(resultado.pendencia().orElseThrow().quantidadeFaltante()).isEqualTo(2);
        assertThat(peca.quantidadeReservada()).isEqualTo(5);
        assertThat(peca.saldoDisponivel()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("deve recusar reserva sem quantidade positiva")
    void deveRecusarReservaNaoPositiva(int quantidade) {
        assertThatThrownBy(() -> comSaldo(24, 0).reservar(UUID.randomUUID(), quantidade))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve registrar a falta de peca como comando da raiz do agregado")
    void deveRegistrarFaltaPelaRaiz() {
        Peca peca = comSaldo(0, 0);
        UUID ordemServicoId = UUID.randomUUID();

        PendenciaPeca pendencia = peca.registrarFalta(ordemServicoId, 3);

        assertThat(pendencia.ordemServicoId()).isEqualTo(ordemServicoId);
        assertThat(pendencia.pecaId()).isEqualTo(peca.id());
        assertThat(pendencia.nomePeca()).isEqualTo(peca.nome());
        assertThat(pendencia.quantidadeFaltante()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("deve recusar registro de falta sem quantidade faltante positiva")
    void deveRecusarFaltaNaoPositiva(int quantidadeFaltante) {
        assertThatThrownBy(() -> comSaldo(0, 0).registrarFalta(UUID.randomUUID(), quantidadeFaltante))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve baixar o Saldo em estoque na retirada da reserva")
    void deveBaixarSaldoNaRetirada() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(UUID.randomUUID(), 4).reserva().orElseThrow();

        peca.retirar(reserva);

        assertThat(reserva.situacao()).isEqualTo(SituacaoReserva.CONSUMIDA);
        assertThat(peca.saldoEmEstoque()).isEqualTo(20);
        assertThat(peca.quantidadeReservada()).isZero();
    }

    @Test
    @DisplayName("deve recusar retirada de reserva que nao esta ATIVA")
    void deveRecusarRetiradaDeReservaNaoAtiva() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(UUID.randomUUID(), 4).reserva().orElseThrow();
        peca.retirar(reserva);

        assertThatThrownBy(() -> peca.retirar(reserva))
                .isInstanceOf(ReservaForaDaSituacaoEsperadaException.class);
        assertThat(peca.saldoEmEstoque()).isEqualTo(20);
    }

    @Test
    @DisplayName("deve liberar a reserva ATIVA na devolucao sem mexer no Saldo em estoque")
    void deveLiberarReservaAtivaNaDevolucao() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(UUID.randomUUID(), 4).reserva().orElseThrow();

        peca.devolver(reserva);

        assertThat(reserva.situacao()).isEqualTo(SituacaoReserva.DEVOLVIDA);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
        assertThat(peca.quantidadeReservada()).isZero();
    }

    @Test
    @DisplayName("deve repor o Saldo em estoque na devolucao de reserva ja CONSUMIDA")
    void deveReporSaldoNaDevolucaoDeReservaConsumida() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(UUID.randomUUID(), 4).reserva().orElseThrow();
        peca.retirar(reserva);

        peca.devolver(reserva);

        assertThat(reserva.situacao()).isEqualTo(SituacaoReserva.DEVOLVIDA);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
        assertThat(peca.quantidadeReservada()).isZero();
    }

    @Test
    @DisplayName("deve recusar devolucao de reserva ja DEVOLVIDA, que somaria saldo duas vezes")
    void deveRecusarDevolucaoRepetida() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca reserva = peca.reservar(UUID.randomUUID(), 4).reserva().orElseThrow();
        peca.retirar(reserva);
        peca.devolver(reserva);

        assertThatThrownBy(() -> peca.devolver(reserva))
                .isInstanceOf(ReservaJaDevolvidaException.class);
        assertThat(peca.saldoEmEstoque()).isEqualTo(24);
    }

    @Test
    @DisplayName("deve recusar movimento de reserva que pertence a outra Peca")
    void deveRecusarReservaDeOutraPeca() {
        Peca peca = comSaldo(24, 0);
        ReservaPeca deOutraPeca = comSaldo(10, 0).reservar(UUID.randomUUID(), 1).reserva().orElseThrow();

        assertThatThrownBy(() -> peca.retirar(deOutraPeca)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> peca.devolver(deOutraPeca)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> peca.retirar(null)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar Saldo em estoque negativo, invariante que o banco tambem protege")
    void deveRecusarSaldoNegativo() {
        assertThatThrownBy(() -> new Peca(UUID.randomUUID(), "Filtro", "unidade", PRECO, -1, 0, 0, true))
                .isInstanceOf(SaldoEmEstoqueNegativoException.class);
    }

    @Test
    @DisplayName("deve recusar quantidade reservada negativa, invariante que o banco tambem protege")
    void deveRecusarReservaNegativa() {
        assertThatThrownBy(() -> new Peca(UUID.randomUUID(), "Filtro", "unidade", PRECO, 5, -1, 0, true))
                .isInstanceOf(QuantidadeReservadaInvalidaException.class);
    }

    @Test
    @DisplayName("deve recusar reserva acima do Saldo em estoque, invariante que o banco tambem protege")
    void deveRecusarReservaAcimaDoSaldo() {
        assertThatThrownBy(() -> new Peca(UUID.randomUUID(), "Filtro", "unidade", PRECO, 2, 3, 0, true))
                .isInstanceOf(QuantidadeReservadaInvalidaException.class);
    }

    @Test
    @DisplayName("deve identificar a peca abaixo do Estoque minimo pelo saldo, com as reservadas dentro")
    void deveIdentificarAbaixoDoEstoqueMinimo() {
        assertThat(comSaldo(1, 1).abaixoDoEstoqueMinimo()).isTrue();
        assertThat(comSaldo(4, 0).abaixoDoEstoqueMinimo()).isFalse();
        assertThat(comSaldo(24, 0).abaixoDoEstoqueMinimo()).isFalse();
    }
}
