package dev.pavel

import akka.actor.{Address, AddressFromURIString}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, JoinSeedNodes}
import com.typesafe.config.ConfigFactory

object ChatCluster {
  var memberName: String = "Some"
  var actorSystem: ActorSystem[Nothing] = _
  var chatMember: ActorRef[Command] = _
  var transmitAdapter: TransmitManager = _

  def startup(role: String, port: Int): Unit = {
    // Override the configuration of the port and role
    val config = ConfigFactory
      .parseString(s"""
        akka.remote.artery.canonical.port=$port
        akka.cluster.roles = [$role]
        """)
      .withFallback(ConfigFactory.load("application"))

    val rootBehavior = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)

      if (cluster.selfMember.hasRole("chatMember")) {
        chatMember = ctx.spawn(ChatActor(), memberName)
      }

      Behaviors.empty
    }
    actorSystem = ActorSystem[Nothing](rootBehavior, "ClusterSystem", config)
  }

  def run(seedNode: String = "127.0.0.1:25251"): Unit = {
    // start cluster
    try {
      if (memberName == null) memberName = "User1"
      startup("chatMember", 25252)
    } catch {
      case _ =>
        try {
          if (memberName == null) memberName = "User2"
          startup("chatMember", 25251)
        } catch {
          case _ =>
            if (memberName == null) memberName = "User3"
            startup("chatMember", 25250)
        }
    }
    val seedNodes: List[Address] =
      List(s"akka://ClusterSystem@$seedNode").map(AddressFromURIString.parse)
    Cluster(actorSystem).manager ! JoinSeedNodes(seedNodes)

  }
}
