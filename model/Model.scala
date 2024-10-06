package com.hungry.wheremoney

import java.time.LocalDate

object Model {
  opaque type CsvRow = List[String]
  object CsvRow:
    def apply(row: List[String]): CsvRow = row
  extension (csvRow: CsvRow) def asListString: List[String] = csvRow.map(_.toString)

  opaque type BankName = String
  object BankName:
    def apply(name: String): BankName = name

  opaque type LastProcessedDate = LocalDate
  object LastProcessedDate:
    def apply(date: LocalDate): LastProcessedDate = date
  extension (date: LastProcessedDate) def asLocalDate: LocalDate = date
}
