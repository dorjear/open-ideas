package com.demo.customer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_GATEWAY, reason = "Unable to retrieve age from Agify")
public class AgifyUnavailableException extends RuntimeException {

    public AgifyUnavailableException() {
        super("Unable to retrieve age from Agify");
    }

    public AgifyUnavailableException(Throwable cause) {
        super("Unable to retrieve age from Agify", cause);
    }
}
