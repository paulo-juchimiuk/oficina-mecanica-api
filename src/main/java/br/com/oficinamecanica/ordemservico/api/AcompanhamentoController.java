package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.application.AprovarOrcamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ConsultarAcompanhamentoUseCase;
import br.com.oficinamecanica.ordemservico.application.ReprovarOrcamentoUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirements
@RequestMapping(AcompanhamentoController.CAMINHO)
public class AcompanhamentoController {

    static final String CAMINHO = "/acompanhamento";

    private final ConsultarAcompanhamentoUseCase consultarAcompanhamento;
    private final AprovarOrcamentoUseCase aprovarOrcamento;
    private final ReprovarOrcamentoUseCase reprovarOrcamento;

    public AcompanhamentoController(ConsultarAcompanhamentoUseCase consultarAcompanhamento,
                                    AprovarOrcamentoUseCase aprovarOrcamento,
                                    ReprovarOrcamentoUseCase reprovarOrcamento) {
        this.consultarAcompanhamento = consultarAcompanhamento;
        this.aprovarOrcamento = aprovarOrcamento;
        this.reprovarOrcamento = reprovarOrcamento;
    }

    @GetMapping("/{codigoAcompanhamento}")
    public AcompanhamentoResponse consultar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(consultarAcompanhamento.executar(codigoAcompanhamento));
    }

    @PostMapping("/{codigoAcompanhamento}/orcamento/aprovacao")
    public AcompanhamentoResponse aprovar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(aprovarOrcamento.executar(codigoAcompanhamento));
    }

    @PostMapping("/{codigoAcompanhamento}/orcamento/reprovacao")
    public AcompanhamentoResponse reprovar(@PathVariable String codigoAcompanhamento) {
        return AcompanhamentoResponse.de(reprovarOrcamento.executar(codigoAcompanhamento));
    }
}
