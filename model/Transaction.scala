package com.hungry.wheremoney

import java.time.LocalDate

import Category.*

final case class Transaction(
    date: LocalDate,
    account: String,
    receiver: String,
    description: String,
    amount: BigDecimal,
    currency: String,
    category: Category,
    categoryKeyword: Option[String]
)
