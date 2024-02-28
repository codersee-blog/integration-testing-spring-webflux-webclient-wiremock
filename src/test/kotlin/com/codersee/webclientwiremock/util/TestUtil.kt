package com.codersee.webclientwiremock.util

import org.springframework.core.io.ClassPathResource

fun getResponseBodyAsString(path: String): String =
    ClassPathResource(path).getContentAsString(
        Charsets.UTF_8,
    )
