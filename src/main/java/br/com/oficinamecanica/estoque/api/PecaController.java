package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.application.AlterarPecaUseCase;
import br.com.oficinamecanica.estoque.application.CadastrarPecaUseCase;
import br.com.oficinamecanica.estoque.application.DetalharPecaUseCase;
import br.com.oficinamecanica.estoque.application.InativarPecaUseCase;
import br.com.oficinamecanica.estoque.application.ListarPecasUseCase;
import br.com.oficinamecanica.estoque.application.RegistrarEntradaEstoqueUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final CadastrarPecaUseCase cadastrarPeca;
    private final AlterarPecaUseCase alterarPeca;
    private final InativarPecaUseCase inativarPeca;
    private final DetalharPecaUseCase detalharPeca;
    private final ListarPecasUseCase listarPecas;
    private final RegistrarEntradaEstoqueUseCase registrarEntradaEstoque;

    public PecaController(CadastrarPecaUseCase cadastrarPeca,
                          AlterarPecaUseCase alterarPeca,
                          InativarPecaUseCase inativarPeca,
                          DetalharPecaUseCase detalharPeca,
                          ListarPecasUseCase listarPecas,
                          RegistrarEntradaEstoqueUseCase registrarEntradaEstoque) {
        this.cadastrarPeca = cadastrarPeca;
        this.alterarPeca = alterarPeca;
        this.inativarPeca = inativarPeca;
        this.detalharPeca = detalharPeca;
        this.listarPecas = listarPecas;
        this.registrarEntradaEstoque = registrarEntradaEstoque;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PecaResponse cadastrar(@Valid @RequestBody PecaRequest requisicao) {
        return PecaResponse.de(cadastrarPeca.executar(
                requisicao.nome(), requisicao.unidadeMedida(),
                requisicao.preco().paraDominio(), requisicao.estoqueMinimo()));
    }

    @GetMapping
    public List<PecaResponse> listar(@RequestParam(defaultValue = "false") boolean abaixoDoMinimo) {
        return listarPecas.executar(abaixoDoMinimo).stream().map(PecaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public PecaResponse detalhar(@PathVariable UUID id) {
        return PecaResponse.de(detalharPeca.executar(id));
    }

    @PutMapping("/{id}")
    public PecaResponse alterar(@PathVariable UUID id, @Valid @RequestBody PecaRequest requisicao) {
        return PecaResponse.de(alterarPeca.executar(
                id, requisicao.nome(), requisicao.unidadeMedida(),
                requisicao.preco().paraDominio(), requisicao.estoqueMinimo()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        inativarPeca.executar(id);
    }

    @PostMapping("/{id}/entradas-estoque")
    public PecaResponse registrarEntrada(@PathVariable UUID id, @Valid @RequestBody EntradaEstoqueRequest requisicao) {
        return PecaResponse.de(registrarEntradaEstoque.executar(id, requisicao.quantidade()));
    }
}
