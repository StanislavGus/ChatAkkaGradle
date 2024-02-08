package dev.pavel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

sealed trait Command

case class Message(text: String, from: String) extends Command with JsonSerializable
case class PrivateMessage(text: String, from: String, toActor: ActorRef[Command]) extends Command with JsonSerializable
case class ChatMembersUpdated(members: Set[ActorRef[Command]]) extends Command with JsonSerializable

object ChatActor {

  val ChatMemberServiceKey = ServiceKey[Command]("ChatMember")

  var membersList: Set[ActorRef[Command]] = Set.empty

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>

      // each worker registers themselves with the receptionist
      ctx.log.info("Registering myself with receptionist")
      ctx.system.receptionist ! Receptionist.Register(ChatMemberServiceKey, ctx.self)

      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
          case ChatActor.ChatMemberServiceKey.Listing(members) =>
            ChatMembersUpdated(members)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(ChatActor.ChatMemberServiceKey, subscriptionAdapter)

      Behaviors.receiveMessage {
        case PrivateMessage(text, from, fromActor) =>
          ChatCluster.transmitAdapter.receiveMessage(text, from, fromActor)
          Behaviors.same
        case Message(text, from) =>
          ChatCluster.transmitAdapter.receiveMessage(text, from, null)
          Behaviors.same
        case ChatMembersUpdated(members) =>
          membersList = members
          ctx.log.info(s"New members: ${members.toString()}")

          ChatCluster.transmitAdapter.setMembersList(members.filter(_ != ctx.self))
          Behaviors.same
        case _ => Behaviors.same
      }
    }
}