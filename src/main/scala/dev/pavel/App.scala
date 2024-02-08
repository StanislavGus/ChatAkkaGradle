package dev.pavel

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.geometry.{Insets, Pos}
import javafx.scene.{Parent, Scene}
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.VBox
import javafx.stage.Stage


object App {
  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(primaryStage: Stage) {
    val loader = new FXMLLoader(getClass().getResource("/chat.fxml"))

    val root: Parent = loader.load() //FXMLLoader.load(getClass().getResource("chat.fxml"))

    val scene = new Scene(root)

    val controller: ChatController = loader.getController()

    val vBox = new VBox
    vBox.setSpacing(10d)
    vBox.setPadding(new Insets(30, 30, 30, 30))
    primaryStage.setTitle("login")
    primaryStage.setScene(new Scene(vBox, 200, 180))
    val label = new Label("Введите имя")
    label.setAlignment(Pos.CENTER)
    vBox.getChildren.add(label)
    val textField = new TextField
    vBox.getChildren.add(textField)
    val labelSeed = new Label("Seed Node")
    labelSeed.setAlignment(Pos.CENTER)
    vBox.getChildren.add(labelSeed)
    val textFieldSeed = new TextField("127.0.0.1:25251")
    vBox.getChildren.add(textFieldSeed)
    val button = new Button("Подтвердить")
    vBox.setAlignment(Pos.CENTER)
    vBox.getChildren.add(button)

    primaryStage.centerOnScreen()

    button.setOnAction { _ =>
      val name = textField.getText
      if (name == null || name == "") controller.memberName = "Anonymous"
      else controller.memberName = name
      controller.showChatTitle(null)
      ChatCluster.memberName = controller.memberName
      ChatCluster.run()
      val transmitAdapter = new TransmitManager(controller)
      controller.setTransmitAdapter(transmitAdapter)
      ChatCluster.transmitAdapter = transmitAdapter
      primaryStage.setScene(scene)
      primaryStage.setTitle("ChatScala")
    }

    primaryStage.setOnHidden(_ => controller.shutdown())
    primaryStage.show()
  }
}
