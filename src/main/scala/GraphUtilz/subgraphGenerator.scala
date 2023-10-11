package com.lsc
package GraphUtilz

import scala.collection.mutable
import NetGraphAlgebraDefs.*
import com.google.common.graph.{Graphs, MutableValueGraph}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

/*
 * This class generates induced subgraphs using the MutableValueGraph guava method
 * and used BFS to traverse for finding the adjacent edges
 * */

class subgraphGenerator(graph: NetGraph) {
  private val visited = mutable.Set[NodeObject]()
  private val result = mutable.ListBuffer[List[NodeObject]]()
  private val inducedGraphs: ListBuffer[MutableValueGraph[NodeObject, Action]] = ListBuffer.empty

  def generateSubGraphs(node: NodeObject, n: Int): List[MutableValueGraph[NodeObject, Action]] = {
    def bfs(queue: mutable.Queue[NodeObject], currentList: mutable.ListBuffer[NodeObject]): Unit = {
      if (queue.isEmpty) {
        return
      }

      val current = queue.dequeue()
      visited += current
      currentList += current

      for (neighbor <- graph.sm.successors(current).asScala) {
        if (!visited.contains(neighbor)) {
          visited += neighbor
          queue.enqueue(neighbor)
          currentList += neighbor
        }
      }

      if (currentList.size >= n) {
        result += currentList.toList

        if (currentList.size > n) {
          queue.enqueue(currentList.last)
        }
        currentList.clear()
      }
      bfs(queue, currentList)
    }

    val queue = mutable.Queue[NodeObject]()
    queue.enqueue(node)
    visited += node
    bfs(queue, mutable.ListBuffer.empty)

    result.toList.foreach { element =>
      val inducedGraph: MutableValueGraph[NodeObject, Action] = Graphs.inducedSubgraph(graph.sm, element.toSet.asJava)
      inducedGraphs += inducedGraph
    }
    inducedGraphs.toList
  }
}
