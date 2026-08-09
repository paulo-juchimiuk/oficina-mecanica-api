package br.com.oficinamecanica.ordemservico.domain;

import java.util.UUID;

public record PecaAIncluir(UUID pecaId, int quantidade, Dinheiro preco) {
}
