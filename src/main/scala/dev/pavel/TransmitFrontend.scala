package dev.pavel

import akka.actor.typed.ActorRef


trait TransmitFrontend {
  def receiveMessage(text: String, from: String, fromActor: ActorRef[Command])

  def updateMembers(members: Set[ActorRef[Command]])

}
