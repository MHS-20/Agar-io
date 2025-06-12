package it.unibo.agar.controller

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.model.World
import it.unibo.agar.view.GlobalView
import scala.concurrent.duration._

object GlobalViewActor {

  sealed trait GlobalViewCommand

  case object PollTick extends GlobalViewCommand

  case class WorldListing(listings: Set[ActorRef[WorldCommand]]) extends GlobalViewCommand

  case class WrappedWorldResponse(world: World) extends GlobalViewCommand

  def apply(globalView: GlobalView): Behavior[GlobalViewCommand] = Behaviors.setup { context =>
    val listingAdapter = context.messageAdapter[Receptionist.Listing] { case listing: Receptionist.Listing =>
      WorldListing(listing.serviceInstances(WorldActor.WorldServiceKey))
    }

    val worldResponseAdapter = context.messageAdapter[WorldResponse](res => WrappedWorldResponse(res.world))
    context.system.receptionist ! Receptionist.Subscribe(WorldActor.WorldServiceKey, listingAdapter)

    Behaviors.withTimers { timers =>
      timers.startTimerAtFixedRate(PollTick, 200.millis)
      behavior(globalView, None, worldResponseAdapter)
    }
  }

  private def behavior(
      globalView: GlobalView,
      managerOpt: Option[ActorRef[WorldCommand]],
      worldResponseAdapter: ActorRef[WorldResponse]
  ): Behavior[GlobalViewCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case WorldListing(managers) =>
          val newManager = managers.headOption.orElse(managerOpt)
          behavior(globalView, newManager, worldResponseAdapter)

        case PollTick =>
          managerOpt.foreach(_ ! RequestWorld(worldResponseAdapter))
          Behaviors.same

        case WrappedWorldResponse(world) =>
          globalView.update(world)
          Behaviors.same
      }
    }
}