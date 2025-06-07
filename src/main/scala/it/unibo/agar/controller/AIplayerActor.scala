package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import it.unibo.agar.model.{AIMovement, Food, World, GameStateManager}
import scala.concurrent.duration._

object AIplayerActor {

  val GameManagerServiceKey: ServiceKey[GameManagerCommand] = ServiceKey[GameManagerCommand]("GameManager")

  def apply(id: String): Behavior[AIPlayerCommand] =
    Behaviors.withTimers { timers =>
      Behaviors.setup[AIPlayerCommand] { context =>

        var gameManagerOpt: Option[ActorRef[GameManagerCommand]] = None
        var lastWorld: Option[World] = None

        // Adapter per ricevere la lista dal Receptionist
        val listingAdapter: ActorRef[Receptionist.Listing] =
          context.messageAdapter { listing =>
            GameManagerListing(listing.serviceInstances(GameManagerServiceKey))
          }

        context.system.receptionist ! Receptionist.Subscribe(GameManagerServiceKey, listingAdapter)

        // timer per AI ticks
        timers.startTimerAtFixedRate(Tick, 200.millis)

        Behaviors.receiveMessage {
          case GameManagerListing(gameManagers) =>
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

          case Tick =>
            (gameManagerOpt, lastWorld) match {
              case (Some(gm), Some(world)) =>
                // Calcolo la mossa con AIMovement
                import it.unibo.agar.model.GameStateManager
                val gameStateManager = new GameStateManager {
                  override def getWorld: World = world

                  override def movePlayerDirection(playerId: String, dx: Double, dy: Double): Unit = {
                    // Invia un messaggio di movimento al gameManager
                    gm ! PlayerMove(playerId, dx, dy)
                  }
                }
                AIMovement.moveAI(id, gameStateManager)
              case _ =>
            }
            Behaviors.same
        }
      }
    }
}
