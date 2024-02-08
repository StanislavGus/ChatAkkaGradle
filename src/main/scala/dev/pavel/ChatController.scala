package dev.pavel

import akka.actor.typed.{ActorRef, ActorSystem}
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{Button, Label, ListCell, ListView, TextArea, TextField}

import scala.collection._
import java.net.URL
import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Date, ResourceBundle}

class ChatController extends Initializable with TransmitFrontend {
  @FXML var sendButton: Button = _
  @FXML var text: TextArea = _
  @FXML var message: TextField = _
  @FXML var chatTitle: Label = _
  @FXML var groupList: ListView[String] = _
  @FXML var personList: ListView[Person] = _

  var memberName: String = null
  var actorSystem: ActorSystem[Nothing] = null
  var history: mutable.Map[String, String] = mutable.Map.empty[String, String]
  var currentReceiver: Person = null
  var persons: mutable.Set[Person] = mutable.Set.empty[Person]
  var transmitAdapter: TransmitManager = null

  @FXML def sendButtonHandler(event: ActionEvent): Unit = {

    showMessage(message.getText, memberName, null, currentReceiver.actor)
    if (currentReceiver.actor == null) transmitAdapter.sendGroupMessage(message.getText)
    else transmitAdapter.sendPrivateMessage(message.getText, currentReceiver)
    message.setText("")
  }

  def showMessage(message: String, from: String = "Anonymous", fromActor: ActorRef[Command] = null, toActor: ActorRef[Command] = null): Unit = {
    val date = new Date
    val df = new SimpleDateFormat("dd-MM-yyyy HH:mm")
    val dateTimeString = df.format(date)
    val textLine = s"\n$dateTimeString [${from}]: ${message}\n"
    if (fromActor == null && toActor == null) {
      history.put("general", history.getOrElse("general", "") + textLine)
    } else if (fromActor != null) {
      history.put(fromActor.path.address.toString, history.getOrElse(fromActor.path.address.toString, "") + textLine)
    } else if (toActor != null) {
      history.put(toActor.path.address.toString, history.getOrElse(toActor.path.address.toString, "") + textLine)
    }
    println(history)
    if (fromActor == currentReceiver.actor || (toActor == currentReceiver.actor && toActor != null)) text.appendText(textLine)
    else if (fromActor != null) {
      persons.foreach(person => {
        person match {
          case Person(name, actor, status, _) if actor == fromActor => {
            person.hasNewMessage = true
          }
          case _ =>
        }
      })
      personList.refresh()
    }
  }

  def showChatTitle(member: ActorRef[Command]) = {
    if (member == null) chatTitle.setText(s"$memberName in GroupChat")
    else chatTitle.setText(s"$memberName in private chat with ${member.path.name}")
  }

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    personList.setCellFactory((param: ListView[Person]) => new ListCell[Person]() {
      override def updateItem(item: Person, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty) {
          setText(null)
        } else if (item == null) {
          setText("GroupChat")
        } else if (item != null){
          val hasNewMessageMark = if (item.hasNewMessage) "• " else ""
          setText(s"$hasNewMessageMark${item.name}(${item.status})")
        } else {
          setText("")
        }
      }
    })
    personList.getSelectionModel().selectedItemProperty().addListener((observable: Any, oldValue: Any, newValue: Person) => {
      showChatTitle(newValue.actor)
      currentReceiver = newValue
      text.clear()
      newValue.hasNewMessage = false
      if (newValue.status == "offline") {
        sendButton.setDisable(true)
      } else {
        sendButton.setDisable(false)
      }
      personList.refresh()
      if (currentReceiver.actor == null) text.appendText(history.getOrElse("general", ""))
      else text.appendText(history.getOrElse(currentReceiver.actor.path.address.toString, ""))
    })
    message.setOnAction(event => sendButtonHandler(event))

    currentReceiver = Person("GroutChat", null, "online")
    chatTitle.setText(s"$memberName in GroupChat")
  }

  def setTransmitAdapter(transmitAdapter: TransmitManager): Unit = {
    this.transmitAdapter = transmitAdapter
  }

  def shutdown() {
    if (ChatCluster.actorSystem != null) ChatCluster.actorSystem.terminate()
  }

  override def receiveMessage(text: String, from: String, fromActor: ActorRef[Command]): Unit = {
    showMessage(text, from, fromActor)
  }

  override def updateMembers(members: Predef.Set[ActorRef[Command]]): Unit = {
    val list = personList.getItems()
    list.clear()
    list.add(Person("GroutChat", null, "online")) // GroupChat

    var memberList = members

    persons.foreach(person => {
      person match {
        case Person(name, actor, status, _) if memberList.contains(actor) => {
          person.status = "online"
          memberList = memberList - actor
        }
        case _ => person.status = "offline"
      }
    })
    memberList.foreach(member => {
      persons.add(Person(member.path.name, member, "online"))
    })
    println(persons)
    persons.foreach(person => list.add(person))
  }
}
