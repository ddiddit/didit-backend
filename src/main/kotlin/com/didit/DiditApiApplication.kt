package com.didit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
class DiditApiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<DiditApiApplication>(*args)
}
