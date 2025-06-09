package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.view.LocalView

object PlayerActor {
  val GameManagerServiceKey: ServiceKey[WorldCommand] = ServiceKey[WorldCommand]("GameManager")

  def apply(id: String): Behavior[PlayerCommand] =
    Behaviors.setup { context =>
      var gameManagerOpt: Option[ActorRef[WorldCommand]] = None
      val localView = new LocalView(id)
      localView.onMouseMoved = (point: java.awt.Point) =>
        val dx = (point.x - localView.size.width / 2) * 0.01
        val dy = (point.y - localView.size.height / 2) * 0.01
        gameManagerOpt.foreach(_ ! PlayerMove(id, dx, dy))

      val listingAdapter: ActorRef[Receptionist.Listing] =
        context.messageAdapter { listing =>
          WorldListing(listing.serviceInstances(GameManagerServiceKey))
        }

      context.system.receptionist ! Receptionist.Subscribe(GameManagerServiceKey, listingAdapter)

      Behaviors.receiveMessage {
        case WorldListing(gameManagers) =>
          gameManagers.headOption match {
            case Some(gmRef) if gameManagerOpt.isEmpty =>
              context.log.info(s"Player $id: trovato GameManager, mi unisco")
              gameManagerOpt = Some(gmRef)
              gmRef ! JoinPlayer(id, context.self)
            case _ => // già registrato o nessun GameManager
          }
          Behaviors.same

        case StartGame() =>
          context.log.info(s"Player $id: game started")
          // Avviare la UI
          localView.open()
          Behaviors.same

        case WorldUpdate(world) =>
          // Aggiornare la view con il nuovo stato del mondo
          localView.updateWorld(world)
          Behaviors.same

        case GameOver(winner) =>
          context.log.info(s"Player $id: game over, winner is $winner")
          localView.showGameOver(winner)
          Behaviors.stopped
      }
    }
}

