package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.application.NotificacaoAoCliente;
import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
class NotificacaoAoClientePorEmail implements NotificacaoAoCliente {

    private static final String ASSUNTO_DO_ORCAMENTO = "Orcamento da sua Ordem de Servico";
    private static final String ASSUNTO_DA_MUDANCA_DE_STATUS = "Atualizacao da sua Ordem de Servico";

    private final MailSender mailSender;
    private final String remetente;
    private final String urlDeAcompanhamento;

    NotificacaoAoClientePorEmail(MailSender mailSender,
                                 @Value("${oficina.email.remetente}") String remetente,
                                 @Value("${oficina.acompanhamento.url-base}") String urlDeAcompanhamento) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlDeAcompanhamento = urlDeAcompanhamento;
    }

    @Override
    public void enviarOrcamento(String email, CodigoAcompanhamento codigoAcompanhamento, Orcamento versao,
                                StatusOrdemServico status) {
        enviar(email, ASSUNTO_DO_ORCAMENTO, corpoDoOrcamento(codigoAcompanhamento, versao, status));
    }

    @Override
    public void enviarMudancaDeStatus(String email, CodigoAcompanhamento codigoAcompanhamento,
                                      StatusOrdemServico status) {
        enviar(email, ASSUNTO_DA_MUDANCA_DE_STATUS, corpoDaMudancaDeStatus(codigoAcompanhamento, status));
    }

    private void enviar(String email, String assunto, String corpo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        mailSender.send(mensagem);
    }

    private String corpoDoOrcamento(CodigoAcompanhamento codigoAcompanhamento, Orcamento versao,
                                    StatusOrdemServico status) {
        return """
                A versao %d do orcamento esta disponivel para sua aprovacao.

                Total: %s %s
                Validade: %d dias

                Situacao atual: %s

                Acompanhe em: %s
                """.formatted(versao.versao(), versao.total().moeda(), versao.total().valor(),
                versao.validadeDias(), status, enderecoDeAcompanhamento(codigoAcompanhamento));
    }

    private String corpoDaMudancaDeStatus(CodigoAcompanhamento codigoAcompanhamento, StatusOrdemServico status) {
        return """
                A sua Ordem de Servico mudou de situacao.

                Situacao atual: %s

                Acompanhe em: %s
                """.formatted(status, enderecoDeAcompanhamento(codigoAcompanhamento));
    }

    private String enderecoDeAcompanhamento(CodigoAcompanhamento codigoAcompanhamento) {
        return urlDeAcompanhamento + "/" + codigoAcompanhamento.valor();
    }
}
