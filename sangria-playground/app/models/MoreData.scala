package models

case class User(
  id: Int,
  name: String,
  localisation: String,
  theme: String
)

case class Page(
  id: Int,
  flows: List[Flow],
  owner: User
)

case class Flow(
  id: Int,
  name: String,
  nodes: List[FlowNode]
)

case class FlowNode(
  id: Int,
  name: String,
  nodeType: String,
  connectedTo: List[FlowNode]
)

class MoreData {
  import models.MoreData._

}

object MoreData {
  var users = List(
    User(
      id = 1,
      name = "Alice",
      localisation = "es-AR",
      theme = "dark"
    ),
    User(
      id = 2,
      name = "Bob",
      localisation = "en-CA",
      theme = "dark"
    ),
    User(
      id = 3,
      name = "Charlie",
      localisation = "en-US",
      theme = "dark"
    ),
  )

  val node3 = FlowNode(
    id = 3,
    name = "Send via SFTP",
    nodeType = "PushSFTP",
    connectedTo = List()
  )

  val node2 = FlowNode(
    id = 2,
    name = "Process the file",
    nodeType = "ProcessFile",
    connectedTo = List(node3)
  )

  val node1 = FlowNode(
    id = 1,
    name = "Get from SFTP",
    nodeType = "PullSFTP",
    connectedTo = List(node2)
  )

  val flow1 = Flow(
    id = 1,
    name = "First flow",
    nodes = List(node1, node2, node3)
  )

  val node4 = FlowNode(
    id = 4,
    name = "Get from SFTP",
    nodeType = "PullSFTP",
    connectedTo = List()
  )

  val flow2 = Flow(
    id = 2,
    name = "Second flow",
    nodes = List(node4)
  )

  val flows = List(flow1, flow2)

  val pages = List(
    Page(
      id = 1,
      flows = List(flow1, flow2),
      owner = users.last
    )
  )
}