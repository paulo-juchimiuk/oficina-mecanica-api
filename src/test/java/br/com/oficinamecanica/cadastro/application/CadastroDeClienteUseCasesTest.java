package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import br.com.oficinamecanica.cadastro.domain.DocumentoJaCadastradoException;
import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("Casos de uso de Cliente")
class CadastroDeClienteUseCasesTest {

    private static final Documento DOCUMENTO = new Documento("10433218100");
    private static final Contato CONTATO = new Contato("cliente@example.com", "11999990000");

    @Mock
    private ClienteRepository clientes;

    @Mock
    private OrdensServicoEmAndamento ordensServico;

    @Test
    @DisplayName("deve cadastrar cliente quando o Documento ainda nao esta em uso")
    void deveCadastrarClienteComDocumentoLivre() {
        when(clientes.documentoJaCadastradoPorOutro(any(), any())).thenReturn(false);
        when(clientes.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Cliente cadastrado = new CadastrarClienteUseCase(clientes).executar("Ana", DOCUMENTO, CONTATO);

        assertThat(cadastrado.ativo()).isTrue();
        assertThat(cadastrado.documento()).isEqualTo(DOCUMENTO);
    }

    @Test
    @DisplayName("deve recusar cadastro quando o Documento pertence a outro cliente, inclusive inativo")
    void deveRecusarDocumentoJaCadastrado() {
        when(clientes.documentoJaCadastradoPorOutro(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> new CadastrarClienteUseCase(clientes).executar("Ana", DOCUMENTO, CONTATO))
                .isInstanceOf(DocumentoJaCadastradoException.class);
        verify(clientes, never()).salvar(any());
    }

    @Test
    @DisplayName("deve recusar alteracao de cliente inexistente ou removido logicamente")
    void deveRecusarAlteracaoDeClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clientes.buscarAtivoParaModificacao(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AlterarClienteUseCase(clientes).executar(id, "Ana", DOCUMENTO, CONTATO))
                .isInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve alterar cliente existente preservando a identidade")
    void deveAlterarClienteExistente() {
        Cliente existente = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        when(clientes.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(clientes.documentoJaCadastradoPorOutro(existente.id(), DOCUMENTO)).thenReturn(false);
        when(clientes.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Cliente alterado = new AlterarClienteUseCase(clientes)
                .executar(existente.id(), "Ana Beatriz", DOCUMENTO, CONTATO);

        assertThat(alterado.id()).isEqualTo(existente.id());
        assertThat(alterado.nome()).isEqualTo("Ana Beatriz");
    }

    @Test
    @DisplayName("deve recusar inativacao quando o cliente tem Ordem de Servico em andamento")
    void deveRecusarInativacaoComOrdemServicoEmAndamento() {
        Cliente existente = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        when(clientes.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaCliente(existente.id())).thenReturn(true);

        assertThatThrownBy(() -> new InativarClienteUseCase(clientes, ordensServico).executar(existente.id()))
                .isInstanceOf(ClienteComOrdemServicoEmAndamentoException.class);
        verify(clientes, never()).salvar(any());
    }

    @Test
    @DisplayName("deve inativar o cliente sem apagar o registro")
    void deveInativarCliente() {
        Cliente existente = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        when(clientes.buscarAtivoParaModificacao(existente.id())).thenReturn(Optional.of(existente));
        when(ordensServico.existemParaCliente(existente.id())).thenReturn(false);

        new InativarClienteUseCase(clientes, ordensServico).executar(existente.id());

        ArgumentCaptor<Cliente> salvo = ArgumentCaptor.forClass(Cliente.class);
        verify(clientes).salvar(salvo.capture());
        assertThat(salvo.getValue().ativo()).isFalse();
    }

    @Test
    @DisplayName("deve recusar detalhamento de cliente inexistente ou removido logicamente")
    void deveRecusarDetalhamentoDeClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clientes.buscarAtivoPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetalharClienteUseCase(clientes).executar(id))
                .isInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("deve listar apenas os ativos quando nao ha filtro por Documento")
    void deveListarAtivosSemFiltro() {
        Cliente ativo = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        when(clientes.listarAtivos()).thenReturn(List.of(ativo));

        assertThat(new ListarClientesUseCase(clientes).executar(Optional.empty())).containsExactly(ativo);
    }

    @Test
    @DisplayName("deve devolver lista vazia quando o filtro por Documento nao acha cliente ativo")
    void deveDevolverListaVaziaQuandoFiltroNaoAcha() {
        when(clientes.buscarAtivoPorDocumento(DOCUMENTO)).thenReturn(Optional.empty());

        assertThat(new ListarClientesUseCase(clientes).executar(Optional.of(DOCUMENTO))).isEmpty();
    }

    @Test
    @DisplayName("deve devolver o cliente do filtro por Documento")
    void deveDevolverClienteDoFiltro() {
        Cliente ativo = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        when(clientes.buscarAtivoPorDocumento(DOCUMENTO)).thenReturn(Optional.of(ativo));

        assertThat(new ListarClientesUseCase(clientes).executar(Optional.of(DOCUMENTO))).containsExactly(ativo);
    }
}
