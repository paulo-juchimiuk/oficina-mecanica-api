package br.com.oficinamecanica.catalogo.api;

import br.com.oficinamecanica.catalogo.domain.Servico;
import java.util.UUID;

public record ServicoResponse(UUID id, String nome, String descricao, DinheiroResponse valorMaoDeObra) {

    static ServicoResponse de(Servico servico) {
        return new ServicoResponse(
                servico.id(),
                servico.nome(),
                servico.descricao(),
                DinheiroResponse.de(servico.valorMaoDeObra()));
    }
}
