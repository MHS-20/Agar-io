package it.unibo.agar.controller

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Food, Player, World}

// GameManager Messagges
sealed trait WorldCommand
final case class PlayerMove(id: String, dx: Double, dy: Double) extends WorldCommand
final case class FoodGenerated(food: Food) extends WorldCommand
final case class JoinPlayer(id: String, replyTo: ActorRef[PlayerCommand]) extends WorldCommand
final case class LeavePlayer(id: String) extends WorldCommand
final case class RequestWorld(replyTo: ActorRef[WorldResponse]) extends WorldCommand
final case class WorldResponse(world: it.unibo.agar.model.World) extends WorldCommand

// Receptionist Listing
sealed trait ReceptionistListingMessage
final case class WorldListing(listings: Set[ActorRef[WorldCommand]]) extends ReceptionistListingMessage with PlayerCommand with AIPlayerCommand with FoodGeneratorCommand

// FoodGenerator Messages
sealed trait FoodGeneratorCommand
case object GenerateFood extends FoodGeneratorCommand

// Player Messages
sealed trait PlayerCommand extends AIPlayerCommand
final case class WorldUpdate(world: World) extends PlayerCommand
final case class StartGame() extends PlayerCommand
final case class GameOver(winner: String) extends PlayerCommand
// final case class GameManagerListing(listings: Set[ActorRef[GameManagerCommand]]) extends PlayerCommand with AIPlayerCommand with FoodGeneratorCommand

// AI-Player Messagges
sealed trait AIPlayerCommand
case object Tick extends AIPlayerCommand