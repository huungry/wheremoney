package com.hungry.wheremoney

import com.github.tototoshi.csv.*

import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import Utils.*
import Transaction.*
import Category.*
import Model.*

class BankHandler(categorizer: Categorizer) {
  enum SupportedBank(
      val name: BankName,
      val csvInputDelimiter: Char,
      val parser: (BankName, CsvRow, Option[LastProcessedDate]) => Option[Transaction]
  ):
    case MILLENIUM
        extends SupportedBank(
          name = BankName("MILLENIUM"),
          csvInputDelimiter = ',',
          parser = milleniumParser
        )
    case ING
        extends SupportedBank(
          name = BankName("ING"),
          csvInputDelimiter = ';',
          parser = ingParser
        )
    case NEST
        extends SupportedBank(
          name = BankName("NEST"),
          csvInputDelimiter = ',',
          parser = nestParser
        )
    case NEST_EUR
        extends SupportedBank(
          name = BankName("NEST_EUR"),
          csvInputDelimiter = ',',
          parser = nestParser
        )
    case REVOLUT
        extends SupportedBank(
          name = BankName("REVOLUT"),
          csvInputDelimiter = ',',
          parser = revolutParser
        )

  // Parsers for each supported bank

  def ingParser(bankName: BankName, csvLine: CsvRow, lastProcessedDateOpt: Option[LastProcessedDate]): Option[Transaction] =
    csvLine.asListString match
      case date :: _ :: receiver :: description :: account :: _ :: kind :: _ :: amount :: currency :: _ =>
        buildTransaction(
          bank = SupportedBank.ING,
          dateTry = Try(LocalDate.parse(date)),
          account = account.replace("'", ""),
          receiver = receiver,
          description = description,
          amountTry = Try(BigDecimal(amount.replace(",", ".").replace(" ", ""))),
          currency = currency,
          onlyAfter = lastProcessedDateOpt
        )

      case invalidRow =>
        logInvalidRow(bankName, invalidRow)
        None

  def milleniumParser(bankName: BankName, csvLine: CsvRow, lastProcessedDateOpt: Option[LastProcessedDate]): Option[Transaction] =
    csvLine.asListString match
      case List(_, date, _, kind, account, receiver, description, debits, credits, _, _) =>
        buildTransaction(
          bank = SupportedBank.MILLENIUM,
          dateTry = Try(LocalDate.parse(date)),
          account = account,
          receiver = receiver,
          description = description,
          amountTry = Try(BigDecimal(debits)).orElse(Try(BigDecimal(credits))),
          currency = "PLN",
          onlyAfter = lastProcessedDateOpt
        )

      case invalidRow =>
        logInvalidRow(bankName, invalidRow)
        None

  def nestParser(bankName: BankName, csvLine: CsvRow, lastProcessedDateOpt: Option[LastProcessedDate]): Option[Transaction] =
    csvLine.asListString match
      case date :: _ :: _ :: amount :: currency :: receiver :: account :: description :: _ =>
        buildTransaction(
          bank = SupportedBank.NEST,
          dateTry = Try(LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"))),
          account = account.replace("'", ""),
          receiver = receiver,
          description = description,
          amountTry = Try(BigDecimal(amount)),
          currency = currency,
          onlyAfter = lastProcessedDateOpt
        )

      case invalid =>
        logInvalidRow(bankName, invalid)
        None

  def revolutParser(bankName: BankName, csvLine: CsvRow, lastProcessedDateOpt: Option[LastProcessedDate]): Option[Transaction] =
    csvLine.asListString match
      case _ :: _ :: date :: _ :: description :: amount :: fee :: currency :: _ =>
        buildTransaction(
          bank = SupportedBank.REVOLUT,
          dateTry = Try(LocalDate.parse(date.split(" ")(0))),
          account = "",
          receiver = "",
          description = description,
          amountTry = Try(BigDecimal(amount)),
          currency = currency,
          onlyAfter = lastProcessedDateOpt
        )

      case invalid =>
        logInvalidRow(bankName, invalid)
        None

  private def logInvalidRow(bank: BankName, invalidRow: List[String]): Unit =
    logYellow(s"[$bank] Skipping non matching row: [$invalidRow]")

  // Process data for each supported bank

  def processBankData(
      bank: SupportedBank
  ): Unit =
    println(s"[$bank] Starting data processing... 🏦")
    println(s"[$bank] Checking last processed date... 📅")
    val lastProcessedDateOpt: Option[LastProcessedDate] = findLastProceccedDate(bank.name)
    val reader = CSVReader.open(new File(s"./inputs/${bank.name}.csv"))(new DefaultCSVFormat {
      override val delimiter = bank.csvInputDelimiter
    })
    val data = reader
      .all()
      .drop(1)
      .flatMap(csvLine => bank.parser(bank.name, CsvRow(csvLine), lastProcessedDateOpt))
    reader.close()
    logUncategorized(data, bank.name)
    exportCsv(data, bank.name)
    logGreen(s"[$bank] Done! 🎉")

  def buildTransaction(
      bank: SupportedBank,
      dateTry: Try[LocalDate],
      account: String,
      receiver: String,
      description: String,
      amountTry: Try[BigDecimal],
      currency: String,
      onlyAfter: Option[LastProcessedDate]
  ): Option[Transaction] = {
    (for {
      date <- dateTry
      amount <- amountTry
    } yield (date, amount, onlyAfter.map(_.asLocalDate).map(date.isAfter))) match
      case Failure(exception) =>
        logRed(s"[${bank.name}] Error processing row with date [$dateTry] and amount [$amountTry] - skipping")
        None
      case Success((date, amount, Some(false))) => None
      case Success((date, amount, _)) =>
        val (category, keyword) = categorizer.categorize(s"$account ${receiver.toLowerCase} ${description.toLowerCase}")
        Some(Transaction(date, account, receiver, description, amount, currency, category, keyword))

  }
}
