package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.application.ConsultarPendenciasDePecasUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@Tag(name = "Pecas")
@RestController
@RequestMapping("/pendencias-pecas")
public class PendenciaPecaController {

    private final ConsultarPendenciasDePecasUseCase consultarPendencias;

    public PendenciaPecaController(ConsultarPendenciasDePecasUseCase consultarPendencias) {
        this.consultarPendencias = consultarPendencias;
    }

    @GetMapping
    @Operation(summary = "Consultar pendencias de pecas")
    public List<PendenciaPecaResponse> listar(@RequestParam(required = false) UUID ordemServicoId) {
        return consultarPendencias.executar(ordemServicoId).stream().map(PendenciaPecaResponse::de).toList();
    }
}
