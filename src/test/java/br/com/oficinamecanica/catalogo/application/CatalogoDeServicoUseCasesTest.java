package br.com.oficinamecanica.catalogo.application;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.catalogo.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.catalogo.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso de Servico")
class CatalogoDeServicoUseCasesTest {

    private static final Dinheiro VALOR = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Mock
    private ServicoRepository servicos;

    @Mock
    private OrdensServicoEmAndamento ordensServico;

    @Test
    @DisplayName("deve cadastrar servico ativo no catalogo")
    void deveCadastrarServico() {
        when(servicos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Servico cadastrado = new CadastrarServicoUseCase(servicos)
                .executar("Troca de oleo", "Inclui filtro", VALOR);

        assertThat(cadastrado.nome()).isEqualTo("Troca de oleo");
        assertThat(cadastrado.valorMaoDeObra()).isEqualTo(VALOR);
        assertThat(cadastrado.ativo()).isTrue();
    }

    @Test
    @DisplayName("deve alterar o servico preservando a identidade")
    void deveAlterarServico() {
        Servico existente = Servico.cadastrar("Troca de oleo", null, VALOR);
        Dinheiro novoValor = new Dinheiro(new BigDecimal("249.00"), "BRL");
        when(servicos.buscarAtivoPorId(existente.id())).thenReturn(Optional.of(existente));
        when(servicos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Servico alterado = new AlterarServicoUseCase(servicos)
                .executar(existente.id(), "Troca de oleo sintetico", "Oleo 5W30", novoValor);

        assertThat(alterado.id()).isEqualTo(existente.id());
        assertThat(alterado.nome()).isEqualTo("Troca de oleo sintetico");
        assertThat(alterado.valorMaoDeObra()).isEqualTo(novoValor);
    }

    @Test
    @DisplayName("deve recusar alteracao de servico inexistente ou removido logicamente")
    void deveRecusarAlteracaoDeServicoInexistente() {
        UUID id = UUID.randomUUID();
        when(servicos.buscarAtivoPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AlterarServicoUseCase(servicos).executar(id, "Troca de oleo", null, VALOR))
                .isInstanceOf(ServicoNaoEncontradoException.class);
        verify(servicos, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar inativacao quando o servico consta em Ordem de Servico em andamento")
    void deveRecusarInativacaoComOrdemServicoEmAndamento() {
        Servico existente = Servico.cadastrar("Troca de oleo", null, VALOR);
        when(servicos.buscarAtivoParaInativacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaServico(existente.id())).thenReturn(true);

        assertThatThrownBy(() -> new InativarServicoUseCase(servicos, ordensServico).executar(existente.id()))
                .isInstanceOf(ServicoComOrdemServicoEmAndamentoException.class);
        verify(servicos, never()).salvar(any());
    }

    @Test
    @DisplayName("deve inativar o servico sem apagar o registro")
    void deveInativarServico() {
        Servico existente = Servico.cadastrar("Troca de oleo", null, VALOR);
        when(servicos.buscarAtivoParaInativacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaServico(existente.id())).thenReturn(false);

        new InativarServicoUseCase(servicos, ordensServico).executar(existente.id());

        ArgumentCaptor<Servico> salvo = ArgumentCaptor.forClass(Servico.class);
        verify(servicos).salvar(salvo.capture());
        assertThat(salvo.getValue().ativo()).isFalse();
    }

    @Test
    @DisplayName("deve recusar inativacao de servico inexistente ou removido logicamente")
    void deveRecusarInativacaoDeServicoInexistente() {
        UUID id = UUID.randomUUID();
        when(servicos.buscarAtivoParaInativacao(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new InativarServicoUseCase(servicos, ordensServico).executar(id))
                .isInstanceOf(ServicoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve recusar detalhamento de servico inexistente ou removido logicamente")
    void deveRecusarDetalhamentoDeServicoInexistente() {
        UUID id = UUID.randomUUID();
        when(servicos.buscarAtivoPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetalharServicoUseCase(servicos).executar(id))
                .isInstanceOf(ServicoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve detalhar o servico ativo")
    void deveDetalharServico() {
        Servico existente = Servico.cadastrar("Troca de oleo", null, VALOR);
        when(servicos.buscarAtivoPorId(existente.id())).thenReturn(Optional.of(existente));

        assertThat(new DetalharServicoUseCase(servicos).executar(existente.id())).isEqualTo(existente);
    }

    @Test
    @DisplayName("deve listar apenas os servicos ativos do catalogo")
    void deveListarAtivos() {
        Servico ativo = Servico.cadastrar("Troca de oleo", null, VALOR);
        when(servicos.listarAtivos()).thenReturn(List.of(ativo));

        assertThat(new ListarServicosUseCase(servicos).executar()).containsExactly(ativo);
    }
}
