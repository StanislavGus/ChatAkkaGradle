package dev.pavel

import akka.actor.typed.ActorRef

case class Person(name: String, actor: ActorRef[Command], var status: String, var hasNewMessage: Boolean = false) {

}
