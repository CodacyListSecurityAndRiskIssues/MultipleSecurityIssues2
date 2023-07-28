package controllers.utils

import codacy.database.accountDB.project.{Project, ProjectIdentifier}
import codacy.database.accountDB.project.deprecated.ProjectTable
import codacy.database.analysisDB.project.{PullRequest, PullRequestIdentifier}
import codacy.database.analysisDB.project.deprecated.PullRequestTable
import codacy.portal.domain.{Error, ErrorCode}
import controllers.JSendHelper
import play.api.libs.json.Writes
import play.api.mvc._
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

object PortalExtensions {
  implicit class ActionExtension(val action: Action.type) extends AnyVal {

    def asyncResult[A](
        body: => Future[Either[Error, A]]
    )(implicit ec: ExecutionContext, writes: Writes[A], codec: Codec): EssentialAction = Action.async {
      body.map(JSendHelper.eitherToResult(_))
    }

    def asyncResult[A, B](bodyParser: BodyParser[B])(
        body: Request[B] => Future[Either[Error, A]]
    )(implicit ec: ExecutionContext, writes: Writes[A], codec: Codec): EssentialAction = Action.async(bodyParser) {
      request =>
        body(request).map(JSendHelper.eitherToResult(_))
    }
  }

  def withProjectAsync[A](
      projectIdentifier: ProjectIdentifier
  )(body: Project => Future[Either[Error, A]]): Future[Either[Error, A]] = {
    ProjectTable
      .getById(projectIdentifier.id)
      .fold(Future.successful(Error(ErrorCode.NotFound, s"Project $projectIdentifier not found").asLeft[A])) {
        project =>
          body(project)
      }
  }

  def withPullRequestAsync[A](
      pullRequestIdentifier: PullRequestIdentifier
  )(body: PullRequest => Future[Either[Error, A]]): Future[Either[Error, A]] = {
    PullRequestTable
      .getById(pullRequestIdentifier.id)
      .fold(Future.successful(Error(ErrorCode.NotFound, s"PullRequest $pullRequestIdentifier not found").asLeft[A])) {
        pullRequest =>
          body(pullRequest)
      }
  }
}
