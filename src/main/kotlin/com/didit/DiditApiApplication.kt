package com.didit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DiditApiApplication

fun main(args: Array<String>) {
    runApplication<DiditApiApplication>(*args)
}
