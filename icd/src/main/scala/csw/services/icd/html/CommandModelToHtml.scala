package csw.services.icd.html

import csw.services.icd.html.HtmlMarkup._
import icd.web.shared.IcdModels.{SendCommandModel, ReceiveCommandModel, CommandModel}

/**
 * Converts a ComponentModel instance to a HTML formatted string
 */
case class CommandModelToHtml(m: CommandModel) extends HtmlMarkup {
  private val name = s"Commands for ${m.component}"
  private val head = mkHeading(2, name)

  private val desc = mkParagraph(m.description)

  private val receive = m.receive.map(ReceiveCommandModelToHtml)

  private val send = SendCommandModelToHtml(m.send)

  override val tags = if (m.receive.nonEmpty || m.send.nonEmpty)
    List(head, desc) ::: receive.map(_.markup) ::: List(send.markup)
  else {
    import scalatags.Text.all._
    List(div())
  }

  override val tocEntry = if (m.receive.nonEmpty || m.send.nonEmpty) {
    import scalatags.Text.all._
    Some(ul(li(a(href := s"#$idStr")(this.name), ul(receive.flatMap(_.tocEntry), send.tocEntry))))
  } else None
}

private case class ReceiveCommandModelToHtml(m: ReceiveCommandModel) extends HtmlMarkup {
  private val name = s"Configuration: ${m.name}"
  private val head = mkHeading(3, name)

  private val desc = mkParagraph(m.description)

  private val requirements = mkParagraph(bold("Requirements:"), m.requirements.mkString(", "))

  private val argsHead = mkParagraph(bold("Arguments:"))

  private val argsTable = mkTable(
    List("Name", "Description", "Type", "Default", "Units", "Required"),
    m.args.map(a => List(a.name, a.description, a.typeStr, a.defaultValue, a.units, yesNo(m.requiredArgs.contains(a.name))))
  )

  override val tags = List(head, requirements, desc, argsHead, argsTable)

  override val tocEntry = Some(mkTocEntry(name))
}

private case class SendCommandModelToHtml(list: List[SendCommandModel]) extends HtmlMarkup {
  private val name = "Configurations Sent to Other Components"
  private val head = mkHeading(3, name)

  private val table = mkTable(
    List("Name", "Component", "Subsystem"),
    list.map(m => List(m.name, m.component, m.subsystem))
  )

  override val tags = if (list.nonEmpty) List(head, table) else {
    import scalatags.Text.all._
    List(div())
  }

  override val tocEntry = if (list.nonEmpty) Some(mkTocEntry(name)) else None
}

