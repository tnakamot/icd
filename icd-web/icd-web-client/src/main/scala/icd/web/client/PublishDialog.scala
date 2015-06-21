package icd.web.client

import icd.web.client.Subsystem.SubsystemWithVersion
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.ext.Ajax
import scala.language.implicitConversions
import org.scalajs.jquery.{ jQuery ⇒ $, _ }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Displays the page for publishing APIs or ICDs
 */
case class PublishDialog(subsystem: Subsystem, targetSubsystem: Subsystem) extends Displayable {

  // Message to display above publish button
  private val messageItem = {
    import scalatags.JsDom.all._
    p("Click below to publish").render
  }

  // Displays upload button
  private val publishButton = {
    import scalatags.JsDom.all._
    button(onclick := publishHandler _)("Publish").render
  }

  // Upload comment box
  private val commentBox = {
    import scalatags.JsDom.all._
    textarea(cls := "form-control", name := "comments", rows := 10, cols := 80).render
  }

  /**
   * Sets the message to display above the Publish button
   */
  private def setMessage(msg: String): Unit = messageItem.textContent = msg

  private def isPublishIcd(s: SubsystemWithVersion, t: SubsystemWithVersion): Boolean =
    s.subsystemOpt.isDefined && t.subsystemOpt.isDefined && t.versionOpt.isDefined && s.versionOpt.isDefined

  private def isPublishApi(s: SubsystemWithVersion, t: SubsystemWithVersion): Boolean =
    s.subsystemOpt.isDefined && t.subsystemOpt.isEmpty && t.versionOpt.isEmpty && s.versionOpt.isEmpty

  /**
   * Called when the source or target subsystem was changed: Update the enabled states
   */
  def subsystemChanged(): Unit = {
    val s = subsystem.getSubsystemWithVersion
    val t = targetSubsystem.getSubsystemWithVersion

    if (isPublishApi(s, t)) {
      val source = s.subsystemOpt.get
      publishButton.disabled = false
      setMessage(s"Click below to publish the $source API")
    } else if (isPublishIcd(s, t)) {
      val source = s.subsystemOpt.get
      publishButton.disabled = false
      val target = t.subsystemOpt.get
      val targetVersion = t.versionOpt.get
      val sourceVersion = s.versionOpt.get
      setMessage(s"Click below to publish the ICD from $source $sourceVersion to $target $targetVersion")
    } else {
      setMessage(s"Please select an unpublished (*) subsystem for the API or published source and target subsystems for an ICD")
      publishButton.disabled = true
    }
  }

  def statusItem = $("#status")

  def busyStatusItem = $("#busyStatus")

  // Called when the publish button is pressed
  def publishHandler(e: dom.Event): Unit = {
    statusItem.addClass("label-default").text("Working...")
    statusItem.removeClass("label-danger")
    busyStatusItem.removeClass("hide")

    val s = subsystem.getSubsystemWithVersion
    val t = targetSubsystem.getSubsystemWithVersion

    if (isPublishApi(s, t)) {
      val majorVersion = false // XXX TODO
      val comment = commentBox.value
      val route = Routes.publishApi(s.subsystemOpt.get, majorVersion, comment)
      Ajax.post(route).map { r ⇒
        subsystem.updateSubsystemVersionOptions()
        displayResultStatus(r)
      }
    } else if (isPublishIcd(s, t)) {

    }
  }

  // Displays the result status of the publish op
  private def displayResultStatus(r: XMLHttpRequest): Unit = {
    busyStatusItem.addClass("hide")
    val statusClass = if (r.status == 200) "label-success" else "label-danger"
    val statusMsg = if (r.status == 200) "Success" else r.statusText
    statusItem.removeClass("label-default").addClass(statusClass).text(statusMsg)
  }

  // Produce the HTML to display for the upload screen
  override def markup(): Element = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div(cls := "container",
      messageItem,
      div(cls := "panel panel-info")(
        div(cls := "panel-body")(
          div(publishButton),
          div(Styles.commentBox, label("Comments")(commentBox)))),
      h4("Status")(
        span(style := "margin-left:15px;"),
        span(id := "busyStatus", cls := "glyphicon glyphicon-refresh glyphicon-refresh-animate hide"),
        span(style := "margin-left:15px;"),
        span(id := "status", cls := "label", "Working..."))).render
  }
}
