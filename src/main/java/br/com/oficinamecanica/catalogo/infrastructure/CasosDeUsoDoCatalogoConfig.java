package br.com.oficinamecanica.catalogo.infrastructure;

import br.com.oficinamecanica.catalogo.application.AlterarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.CadastrarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.DetalharServicoUseCase;
import br.com.oficinamecanica.catalogo.application.InativarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.ListarServicosUseCase;
import br.com.oficinamecanica.catalogo.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CasosDeUsoDoCatalogoConfig {

    @Bean
    AlterarServicoUseCase alterarServicoUseCase(ServicoRepository servicos) {
        return new AlterarServicoUseCase(servicos);
    }

    @Bean
    CadastrarServicoUseCase cadastrarServicoUseCase(ServicoRepository servicos) {
        return new CadastrarServicoUseCase(servicos);
    }

    @Bean
    DetalharServicoUseCase detalharServicoUseCase(ServicoRepository servicos) {
        return new DetalharServicoUseCase(servicos);
    }

    @Bean
    InativarServicoUseCase inativarServicoUseCase(ServicoRepository servicos, OrdensServicoEmAndamento ordensServico) {
        return new InativarServicoUseCase(servicos, ordensServico);
    }

    @Bean
    ListarServicosUseCase listarServicosUseCase(ServicoRepository servicos) {
        return new ListarServicosUseCase(servicos);
    }
}
