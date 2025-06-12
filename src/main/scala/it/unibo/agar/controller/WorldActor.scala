package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import it.unibo.agar.model.{EatingManager, Food, Player, World}
import scala.util.Random


object WorldActor {
  val WorldServiceKey: ServiceKey[WorldCommand] = ServiceKey[WorldCommand]("GameManager")

  def apply(initialPlayers: Seq[Player], initialFood: Seq[Food]): Behavior[WorldCommand] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(WorldServiceKey, context.self)
    var players = Map.empty[String, ActorRef[PlayerCommand]]
    var world = World(width = 1000, height = 1000, players = initialPlayers, foods = initialFood)

    def broadcastWorld(): Unit =
      players.values.foreach(_ ! WorldUpdate(world))

    Behaviors.receiveMessage {
      case JoinPlayer(id, replyTo) =>
        context.log.info(s"Player $id joined")
        players += (id -> replyTo)
        // Add new player to world
        val newPlayer = Player(id, x =  Random.nextInt(1000), y = Random.nextInt(1000), mass = 120)
        world = world.copy(players = world.players :+ newPlayer)
        broadcastWorld()
        replyTo ! StartGame()
        Behaviors.same

      case LeavePlayer(id) =>
        context.log.info(s"Player $id left")
        players -= id
        world = world.copy(players = world.players.filterNot(_.id == id))
        broadcastWorld()
        Behaviors.same

      case PlayerMove(id, dx, dy) =>
        // Update player position and handle eating
        world.playerById(id) match {
          case Some(player) =>
            val newX = (player.x + dx * 10).max(0).min(world.width)
            val newY = (player.y + dy * 10).max(0).min(world.height)
            val movedPlayer = player.copy(x = newX, y = newY)

            // Check for food eaten
            val eatenFoods = world.foods.filter(food => EatingManager.canEatFood(movedPlayer, food))
            val updatedPlayer = eatenFoods.foldLeft(movedPlayer)((p, f) => p.grow(f))
            val remainingFoods = world.foods.filterNot(eatenFoods.contains)

            val eatenPlayers = world.players.filter { other =>
              other.id != id && EatingManager.canEatPlayer(updatedPlayer, other)}
            val playerAfterPlayers = eatenPlayers.foldLeft(updatedPlayer)((p, other) => p.grow(other))
            val remainingPlayers = world.players.filterNot(p => eatenPlayers.contains(p) || p.id == id) :+ playerAfterPlayers

            // Update players and foods
//            val updatedPlayers = world.players.map {
//              case p if p.id == id => updatedPlayer
//              case other => other
//            }

            world = world.copy(players = remainingPlayers, foods = remainingFoods)
            broadcastWorld()

            /* world.players.find(_.mass > 10000) match {
              case Some(winner) =>
                players.values.foreach(_ ! GameOver(winner.id))
                Behaviors.stopped
              case None =>
                Behaviors.same
            } */

          case None => // Player not found
        }
        Behaviors.same

      case FoodGenerated(food) =>
        world = world.copy(foods = world.foods :+ food)
        broadcastWorld()
        Behaviors.same

      case RequestWorld(replyTo) =>
        replyTo ! WorldResponse(world)
        Behaviors.same

      case TriggerGameOver(winnerId) =>
        players.values.foreach(_ ! GameOver(winnerId))
        Behaviors.stopped

    }
  }
}
