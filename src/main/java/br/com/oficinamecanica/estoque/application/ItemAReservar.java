package br.com.oficinamecanica.estoque.application;

import java.util.UUID;

public record ItemAReservar(UUID pecaId, int quantidade) {
}
