package com.hungry.wheremoney

import java.time.LocalDate

import Category.*

final class Categorizer(categories: Map[Category, List[String]]) {
  extension (string: String) def normalized: String = string.replace(" ", "").toLowerCase

  def categorize(transactionData: String): (Category, Option[String]) = {
    val normalizedData = transactionData.normalized

    categories
      .collectFirst {
        case (category, keywords) if keywords.exists(keyword => normalizedData.contains(keyword.normalized)) =>
          (category, keywords.find(keyword => normalizedData.contains(keyword.normalized)))
      }
      .getOrElse((Category.Uncategorized, None))
  }
}
