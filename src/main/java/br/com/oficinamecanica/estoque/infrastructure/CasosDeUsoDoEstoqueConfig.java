package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.application.AlterarPecaUseCase;
import br.com.oficinamecanica.estoque.application.CadastrarPecaUseCase;
import br.com.oficinamecanica.estoque.application.ConsultarPecasReservadasUseCase;
import br.com.oficinamecanica.estoque.application.ConsultarPendenciasDePecasUseCase;
import br.com.oficinamecanica.estoque.application.DetalharPecaUseCase;
import br.com.oficinamecanica.estoque.application.DevolverPecasNaoUtilizadasUseCase;
import br.com.oficinamecanica.estoque.application.InativarPecaUseCase;
import br.com.oficinamecanica.estoque.application.ListarPecasUseCase;
import br.com.oficinamecanica.estoque.application.RegistrarEntradaEstoqueUseCase;
import br.com.oficinamecanica.estoque.application.RegistrarFaltaDePecaUseCase;
import br.com.oficinamecanica.estoque.application.ReservarPecasUseCase;
import br.com.oficinamecanica.estoque.application.RetirarPecasReservadasUseCase;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CasosDeUsoDoEstoqueConfig {

    @Bean
    AlterarPecaUseCase alterarPecaUseCase(PecaRepository pecas) {
        return new AlterarPecaUseCase(pecas);
    }

    @Bean
    CadastrarPecaUseCase cadastrarPecaUseCase(PecaRepository pecas) {
        return new CadastrarPecaUseCase(pecas);
    }

    @Bean
    ConsultarPecasReservadasUseCase consultarPecasReservadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        return new ConsultarPecasReservadasUseCase(pecas, ordensServico);
    }

    @Bean
    ConsultarPendenciasDePecasUseCase consultarPendenciasDePecasUseCase(PecaRepository pecas) {
        return new ConsultarPendenciasDePecasUseCase(pecas);
    }

    @Bean
    DetalharPecaUseCase detalharPecaUseCase(PecaRepository pecas) {
        return new DetalharPecaUseCase(pecas);
    }

    @Bean
    DevolverPecasNaoUtilizadasUseCase devolverPecasNaoUtilizadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        return new DevolverPecasNaoUtilizadasUseCase(pecas, ordensServico);
    }

    @Bean
    InativarPecaUseCase inativarPecaUseCase(PecaRepository pecas, OrdensServicoEmAndamento ordensServicoEmAndamento) {
        return new InativarPecaUseCase(pecas, ordensServicoEmAndamento);
    }

    @Bean
    ListarPecasUseCase listarPecasUseCase(PecaRepository pecas) {
        return new ListarPecasUseCase(pecas);
    }

    @Bean
    RegistrarEntradaEstoqueUseCase registrarEntradaEstoqueUseCase(PecaRepository pecas) {
        return new RegistrarEntradaEstoqueUseCase(pecas);
    }

    @Bean
    RegistrarFaltaDePecaUseCase registrarFaltaDePecaUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        return new RegistrarFaltaDePecaUseCase(pecas, ordensServico);
    }

    @Bean
    ReservarPecasUseCase reservarPecasUseCase(PecaRepository pecas) {
        return new ReservarPecasUseCase(pecas);
    }

    @Bean
    RetirarPecasReservadasUseCase retirarPecasReservadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        return new RetirarPecasReservadasUseCase(pecas, ordensServico);
    }
}
