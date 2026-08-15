package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.application.EnvioDeOrcamento;
import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Component;

@Component
class EnvioDeOrcamentoPorEmail implements EnvioDeOrcamento {

    private static final String ASSUNTO = "Orcamento da sua Ordem de Servico";

    private final MailSender mailSender;
    private final String remetente;
    private final String urlDeAcompanhamento;

    EnvioDeOrcamentoPorEmail(MailSender mailSender,
                             @Value("${oficina.email.remetente}") String remetente,
                             @Value("${oficina.acompanhamento.url-base}") String urlDeAcompanhamento) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlDeAcompanhamento = urlDeAcompanhamento;
    }

    @Override
    public void enviar(String email, CodigoAcompanhamento codigoAcompanhamento, Orcamento versao) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject(ASSUNTO);
        mensagem.setText(corpo(codigoAcompanhamento, versao));
        mailSender.send(mensagem);
    }

    private String corpo(CodigoAcompanhamento codigoAcompanhamento, Orcamento versao) {
        return """
                A versao %d do orcamento esta disponivel para sua aprovacao.

                Total: %s %s
                Validade: %d dias

                Acompanhe em: %s/%s
                """.formatted(versao.versao(), versao.total().moeda(), versao.total().valor(),
                versao.validadeDias(), urlDeAcompanhamento, codigoAcompanhamento.valor());
    }
}
