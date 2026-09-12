package br.com.oficinamecanica.shared.infrastructure;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
class TransacaoDosCasosDeUsoConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    BeanFactoryTransactionAttributeSourceAdvisor transacaoDosCasosDeUso(TransactionManager gerenciadorDeTransacao) {
        TransactionAttributeSource atributos = new AtributosDeTransacaoDosCasosDeUso();
        BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        advisor.setTransactionAttributeSource(atributos);
        advisor.setAdvice(new TransactionInterceptor(gerenciadorDeTransacao, atributos));
        return advisor;
    }
}
