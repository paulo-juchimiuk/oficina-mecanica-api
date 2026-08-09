package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.domain.Peca;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        String nome,
        String unidadeMedida,
        DinheiroResponse preco,
        int estoqueMinimo,
        int saldoEmEstoque,
        int quantidadeReservada) {

    static PecaResponse de(Peca peca) {
        return new PecaResponse(
                peca.id(),
                peca.nome(),
                peca.unidadeMedida(),
                DinheiroResponse.de(peca.preco()),
                peca.estoqueMinimo(),
                peca.saldoEmEstoque(),
                peca.quantidadeReservada());
    }
}
