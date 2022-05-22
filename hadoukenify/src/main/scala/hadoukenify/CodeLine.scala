package hadoukenify

final case class CodeLine(text: String, indent: Int) {
  val unintendedText: String = text.trim
}

object CodeLine {
  def parseCode(code: String): Array[CodeLine] =
    normalizeCode(code).linesIterator.map { s =>
      val indent = s.length - s.trim.length
      CodeLine(s, indent)
    }.toArray

  private def normalizeCode(code: String): String =
    code.replace("\t", "    ")
}
