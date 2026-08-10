package br.com.oficinamecanica.catalogo.api;

import br.com.oficinamecanica.catalogo.application.AlterarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.CadastrarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.DetalharServicoUseCase;
import br.com.oficinamecanica.catalogo.application.InativarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.ListarServicosUseCase;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@Tag(name = "Servicos")
@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final CadastrarServicoUseCase cadastrarServico;
    private final AlterarServicoUseCase alterarServico;
    private final InativarServicoUseCase inativarServico;
    private final DetalharServicoUseCase detalharServico;
    private final ListarServicosUseCase listarServicos;

    public ServicoController(CadastrarServicoUseCase cadastrarServico,
                             AlterarServicoUseCase alterarServico,
                             InativarServicoUseCase inativarServico,
                             DetalharServicoUseCase detalharServico,
                             ListarServicosUseCase listarServicos) {
        this.cadastrarServico = cadastrarServico;
        this.alterarServico = alterarServico;
        this.inativarServico = inativarServico;
        this.detalharServico = detalharServico;
        this.listarServicos = listarServicos;
    }

    @PostMapping
    @Operation(summary = "Cadastrar servico")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponse cadastrar(@Valid @RequestBody ServicoRequest requisicao) {
        return ServicoResponse.de(cadastrarServico.executar(
                requisicao.nome(), requisicao.descricao(), requisicao.valorMaoDeObra().paraDominio()));
    }

    @GetMapping
    @Operation(summary = "Listar servicos")
    public List<ServicoResponse> listar() {
        return listarServicos.executar().stream().map(ServicoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar servico")
    public ServicoResponse detalhar(@PathVariable UUID id) {
        return ServicoResponse.de(detalharServico.executar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar servico")
    public ServicoResponse alterar(@PathVariable UUID id, @Valid @RequestBody ServicoRequest requisicao) {
        return ServicoResponse.de(alterarServico.executar(
                id, requisicao.nome(), requisicao.descricao(), requisicao.valorMaoDeObra().paraDominio()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover servico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        inativarServico.executar(id);
    }
}
