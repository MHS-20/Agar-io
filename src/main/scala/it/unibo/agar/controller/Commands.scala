package it.unibo.agar.controller

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Food, Player, World}

// Messaggi GameManager
sealed trait GameManagerCommand
final case class PlayerMove(id: String, dx: Double, dy: Double) extends GameManagerCommand
final case class FoodGenerated(food: Food) extends GameManagerCommand
final case class JoinPlayer(id: String, replyTo: ActorRef[PlayerCommand]) extends GameManagerCommand
final case class LeavePlayer(id: String) extends GameManagerCommand
final case class RequestWorld(replyTo: ActorRef[WorldResponse]) extends GameManagerCommand
final case class WorldResponse(world: it.unibo.agar.model.World)

sealed trait ReceptionistListingMessage
final case class GameManagerListing(listings: Set[ActorRef[GameManagerCommand]]) extends ReceptionistListingMessage with PlayerCommand with AIPlayerCommand with FoodGeneratorCommand

// Messaggi FoodGenerator
sealed trait FoodGeneratorCommand
case object GenerateFood extends FoodGeneratorCommand

// Messaggi Player
sealed trait PlayerCommand extends AIPlayerCommand
final case class WorldUpdate(world: World) extends PlayerCommand
final case class StartGame() extends PlayerCommand
// final case class GameManagerListing(listings: Set[ActorRef[GameManagerCommand]]) extends PlayerCommand with AIPlayerCommand with FoodGeneratorCommand 

// Messaggi AIPlayer
sealed trait AIPlayerCommand
case object Tick extends AIPlayerCommand