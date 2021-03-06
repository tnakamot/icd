package controllers

import com.mongodb.MongoTimeoutException
import com.typesafe.config.ConfigException
import csw.services.icd.db.StdConfig
import csw.services.icd.{IcdValidator, Problem}
import play.api.mvc.{Action, Controller, Result}

class FileUploadController extends Controller {

  private val log = play.Logger.of("application")
  private lazy val db = Application.db

  // Server side of the upload ICD feature.
  // Supported file types: A directory containing icd config files (chrome)
  // or a .zip file containing directories with icd config files.
  def uploadFiles = Action(parse.multipartFormData) { request =>
    import upickle.default._
    val files = request.body.files.toList
    try {
      // XXX TODO: Return config parse errors in StdConfig.get with file names!
      val list = files.flatMap(filePart => StdConfig.get(filePart.ref.file, filePart.filename))
      val comment = request.body.asFormUrlEncoded.getOrElse("comment", List("")).head
      ingestConfigs(list, comment)
    } catch {
      case e: MongoTimeoutException =>
        val msg = "Database seems to be down"
        log.error(msg, e)
        ServiceUnavailable(write(List(Problem("error", msg)))).as(JSON)
      case e: ConfigException =>
        val msg = e.getMessage
        log.error(msg, e)
        NotAcceptable(write(List(Problem("error", msg)))).as(JSON)
      case t: Throwable =>
        val msg = "Internal error"
        log.error(msg, t)
        InternalServerError(write(List(Problem("error", msg)))).as(JSON)
    }
  }

  /**
   * Uploads/ingests the given API config files
   *
   * @param list         list of objects based on uploaded ICD files
   * @param comment      change comment from user
   * @return the HTTP result (OK, or NotAcceptable[list of Problems in JSON format])
   */
  private def ingestConfigs(list: List[StdConfig], comment: String): Result = {
    import upickle.default._
    // Validate everything first
    val validateProblems = list.flatMap(sc => IcdValidator.validate(sc.config, sc.stdName))
    if (validateProblems.nonEmpty) {
      NotAcceptable(write(validateProblems)).as(JSON)
    } else {
      val problems = list.flatMap(db.ingestConfig)
      if (problems.nonEmpty) {
        NotAcceptable(write(problems)).as(JSON)
      } else {
        Ok.as(JSON)
      }
    }
  }
}
