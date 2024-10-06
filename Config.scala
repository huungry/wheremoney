package com.hungry.wheremoney

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

import java.time.LocalDate

final case class Config(keywords: Map[String, List[String]])

object Config {
  def unsafeParse() = io.circe.parser.parse(scala.io.Source.fromFile("./config.json").mkString) match
    case Left(value) => throw new Exception(s"Error parsing config.json: $value")
    case Right(value) =>
      value.as[Config] match
        case Left(value)  => throw new Exception(s"Error decoding config.json: $value")
        case Right(value) => value

  implicit val configDecoder: Decoder[Config] = deriveDecoder
}
