package com.dedicatedcode.paperspace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Set;

/**
 * Created by Daniel Wasilew on 09.11.15.
 * © 2016 bessering All Rights Reserved
 */
@Configuration
public class ThymeleafPDFTemplateExtension {

    @Autowired
    private TemplateEngine templateEngine;


    @Bean(name = "textTemplateEngine")
    public TemplateEngine textTemplateEngine(MessageSource messageSource, Set<IDialect> dialects) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(textTemplateResolver());
        templateEngine.setMessageSource(messageSource);
        templateEngine.setAdditionalDialects(dialects);
        return templateEngine;
    }

    private ITemplateResolver textTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("mail-templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF8");
        templateResolver.setCheckExistence(true);
        templateResolver.setCacheable(false);
        return templateResolver;
    }
}
