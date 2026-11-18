package com.wersiv.datingsite.backend.web;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = PageController.class)
@RequiredArgsConstructor
public class PageExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(LocalizedException.class)
    public ModelAndView localized(LocalizedException exception) {
        String text = messageSource.getMessage(exception.getCode(), exception.getArgs(), exception.getCode(), LocaleContextHolder.getLocale());
        return error(exception.getStatus(), text);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MaxUploadSizeExceededException.class})
    public ModelAndView invalidRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Проверьте введённые данные. Максимальный размер фотографии — 5 МБ.");
    }

    private ModelAndView error(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("page-error");
        view.setStatus(status);
        view.addObject("error", message);
        return view;
    }
}
