package icd.web.client

import org.scalajs.dom
import org.scalajs.dom._
import upickle._

// Type of a view in the application, used to restore the view
// Note: Needs to be outside of object, due to scala.js restrictions
sealed trait ViewType

// Viewing components based on checkbox state in sidebar
case object ComponentView extends ViewType

// Viewing component from a publisher/subscriber/command link
case object ComponentLinkView extends ViewType

// Uploading ICD
case object UploadView extends ViewType

// Result of View menu => Static API as HTML Document
case object HtmlView extends ViewType

// Result of View menu => Static API as PDF Document
case object PdfView extends ViewType

/**
 * Object used to keep track of browser history for back button
 *
 * @param sourceSubsystem subsystem selected in the left box
 * @param targetSubsystem subsystem selected in the right box
 * @param sourceComponents source subsystem components whose checkboxes are checked
 * @param linkComponent set to the name of the component displayed via a subscriber/publisher/command link
 * @param viewType indicates the type of data being displayed
 */
case class BrowserHistory(sourceSubsystem: Option[String], targetSubsystem: Option[String],
                          sourceComponents: List[String], linkComponent: Option[String],
                          viewType: ViewType) {

  // Pushes the current application history state (Note that the title is ignored in some browsers)
  def pushState(): Unit = {
    val json = write(this)
    dom.history.pushState(json, dom.document.title, dom.document.documentURI)
  }
}

object BrowserHistory {

  // Gets  BrowserHistory from the event
  def popState(e: PopStateEvent): Option[BrowserHistory] = {
    if (e.state == null) None
    else Some(read[BrowserHistory](e.state.toString))
  }
}
