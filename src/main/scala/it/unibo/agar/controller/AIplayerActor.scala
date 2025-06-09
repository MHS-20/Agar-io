package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import it.unibo.agar.model.{AIMovement, Food, World, GameStateManager}
import scala.concurrent.duration._

object AIplayerActor {

  val GameManagerServiceKey: ServiceKey[WorldCommand] = ServiceKey[WorldCommand]("GameManager")

  def apply(id: String): Behavior[AIPlayerCommand] =
    Behaviors.withTimers { timers =>
      Behaviors.setup[AIPlayerCommand] { context =>

        var gameManagerOpt: Option[ActorRef[WorldCommand]] = None
        var lastWorld: Option[World] = None

        // Adapter per ricevere la lista dal Receptionist
        val listingAdapter: ActorRef[Receptionist.Listing] =
          context.messageAdapter { listing =>
            WorldListing(listing.serviceInstances(GameManagerServiceKey))
          }

        context.system.receptionist ! Receptionist.Subscribe(GameManagerServiceKey, listingAdapter)

        // timer per AI ticks
        timers.startTimerAtFixedRate(Tick, 100.millis)

        Behaviors.receiveMessage {
          case WorldListing(gameManagers) =>
            gameManagers.headOption match {
              case Some(gm) if gameManagerOpt.isEmpty =>
                context.log.info(s"AIPlayer $id: trovato GameManager, mi unisco")
                gameManagerOpt = Some(gm)
                gm ! JoinPlayer(id, context.self.narrow[PlayerCommand])
              case _ =>
            }
            Behaviors.same

          case WorldUpdate(world) =>
            lastWorld = Some(world)
            Behaviors.same

          case StartGame() =>
            context.log.info(s"AIPlayer $id: game started")
            // Inizializzare la logica di gioco, se necessario
            Behaviors.same

          case Tick =>
            for {
              gm <- gameManagerOpt
              world <- lastWorld
              (dx, dy) <- AIMovement.computeDirectionTowardNearestFood(id, world)
            } gm ! PlayerMove(id, dx, dy)
            Behaviors.same
            
          case GameOver(winner) =>
            context.log.info(s"AIPlayer $id: game over, winner is $winner")
            Behaviors.stopped
        }
      }
    }
}
