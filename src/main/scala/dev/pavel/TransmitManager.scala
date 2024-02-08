package dev.pavel

import akka.actor.typed.ActorRef

class TransmitManager(frontend: TransmitFrontend) {
  var membersList: Set[ActorRef[Command]] = Set.empty

  def sendPrivateMessage(message: String, person: Person): Unit = {
    person.actor ! PrivateMessage(message, person.name, ChatCluster.chatMember)
  }
  def sendGroupMessage(message: String): Unit = {
    membersList.foreach(member =>
      if (member != ChatCluster.chatMember) member ! Message(message, ChatCluster.memberName)
    )
  }

  def receiveMessage(message: String, from: String, fromActor: ActorRef[Command]): Unit = {
    frontend.receiveMessage(message, from, fromActor)
  }

  def setMembersList(list: Set[ActorRef[Command]]): Unit = {
    membersList = list
    frontend.updateMembers(membersList)
  }
}
