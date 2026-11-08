package br.com.agendaflex.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ModelAndView mv = new ModelAndView("erro");
        mv.addObject("tituloErro", "Não encontramos o que você procura");
        mv.addObject("mensagemErro", ex.getMessage());
        return mv;
    }

    /** Rede de segurança: os controllers tratam as regras de negócio nos formulários. */
    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ModelAndView regraDeNegocio(RegraNegocioException ex) {
        ModelAndView mv = new ModelAndView("erro");
        mv.addObject("tituloErro", "Não foi possível concluir a ação");
        mv.addObject("mensagemErro", ex.getMessage());
        return mv;
    }
}
