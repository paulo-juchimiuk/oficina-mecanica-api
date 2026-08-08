package br.com.oficinamecanica.autenticacao.application;

import java.time.Instant;

public record TokenJwt(String token, Instant expiraEm) {
}
