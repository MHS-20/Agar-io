package it.unibo.agar.controller

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import it.unibo.agar.model.{EatingManager, Food, Player, World}

object GameManagerActor {
  val GameManagerServiceKey: ServiceKey[GameManagerCommand] = ServiceKey[GameManagerCommand]("GameManager")

  def apply(initialPlayers: Seq[Player], initialFood: Seq[Food]): Behavior[GameManagerCommand] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(GameManagerServiceKey, context.self)
    var players = Map.empty[String, ActorRef[PlayerCommand]]
    var world = World(width = 1000, height = 1000, players = initialPlayers, foods = initialFood)

    def broadcastWorld(): Unit =
      players.values.foreach(_ ! WorldUpdate(world))

    Behaviors.receiveMessage {
      case JoinPlayer(id, replyTo) =>
        context.log.info(s"Player $id joined")
        players += (id -> replyTo)
        // Add new player to world
        val newPlayer = Player(id, x = 0, y = 0, mass = 100)
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

            // Remove eaten foods from world
            val remainingFoods = world.foods.filterNot(eatenFoods.contains)
            // Update players and foods
            val updatedPlayers = world.players.map {
              case p if p.id == id => updatedPlayer
              case other => other
            }

            world = world.copy(players = updatedPlayers, foods = remainingFoods)
            broadcastWorld()

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
    }
  }
}
