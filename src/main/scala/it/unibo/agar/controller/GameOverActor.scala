package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import it.unibo.agar.model.World

import scala.concurrent.duration._

object GameOverActor {

  sealed trait GameOverCommand

  private case object PollTick extends GameOverCommand
  private case class WorldListing(listings: Set[ActorRef[WorldCommand]]) extends GameOverCommand
  private case class WrappedWorldResponse(world: World) extends GameOverCommand

  def apply(): Behavior[GameOverCommand] = Behaviors.setup { context =>
    val listingAdapter = context.messageAdapter[Receptionist.Listing] {
      case listing => WorldListing(listing.serviceInstances(WorldActor.WorldServiceKey))
    }

    val worldResponseAdapter = context.messageAdapter[WorldResponse](res => WrappedWorldResponse(res.world))

    // Subscribe to WorldActor via Receptionist
    context.system.receptionist ! Receptionist.Subscribe(WorldActor.WorldServiceKey, listingAdapter)

    Behaviors.withTimers { timers =>
      timers.startTimerAtFixedRate(PollTick, 200.millis)
      behavior(None, worldResponseAdapter)
    }
  }

  private def behavior(
                        worldOpt: Option[ActorRef[WorldCommand]],
                        worldResponseAdapter: ActorRef[WorldResponse]
                      ): Behavior[GameOverCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case WorldListing(worlds) =>
          val newWorld = worlds.headOption.orElse(worldOpt)
          behavior(newWorld, worldResponseAdapter)

        case PollTick =>
          worldOpt.foreach(_ ! RequestWorld(worldResponseAdapter))
          Behaviors.same

        case WrappedWorldResponse(world) =>
          world.players.find(_.mass > 10000) match {
            case Some(winner) =>
              context.log.info(s"[GameOverActor] Winner is ${winner.id}")
              worldOpt.foreach(_ ! TriggerGameOver(winner.id))
              Behaviors.stopped
            case None =>
              Behaviors.same
          }
      }
    }
}
