package com.fridgechef.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Names of the flash attributes rendered by the layout. */
final class FlashMessages {

    static final String SUCCESS_ATTRIBUTE = "successMessage";
    static final String ERROR_ATTRIBUTE = "errorMessage";

    private FlashMessages() {
    }

    static void success(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE, message);
    }

    static void error(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, message);
    }
}
