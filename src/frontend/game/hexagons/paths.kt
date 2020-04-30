package frontend.game.hexagons

data class Step(val start: HexCubeCoord, val finish: HexCubeCoord, val heading: HexDirection)

data class Path(val steps: List<Step>) {
    val size: Int
        get() = steps.size

    override fun toString(): String {
        return steps.map { it.heading }
            .joinToString(" -> ")
    }
}

private data class Node(val coord: HexCubeCoord, val heading: HexDirection) {
    fun fromCoord(coord: HexCubeCoord): Node {
        return Node(coord, headingFromCoords(coord, this.coord))
    }
}

fun headingFromCoords(from: HexCubeCoord, to: HexCubeCoord): HexDirection {
    return if (from == to) {
        error("tried to find the heading between a point and itself")
    } else if (from.x == to.x) {
        if (to.z < from.z) {
            HexDirection.Top
        } else {
            HexDirection.Bottom
        }
    } else if (from.y == to.y) {
        if (to.z < from.z) {
            HexDirection.TopRight
        } else {
            HexDirection.BottomLeft
        }
    } else if (from.z == to.z) {
        if (to.x < from.x) {
            HexDirection.TopLeft
        } else {
            HexDirection.BottomRight
        }
    } else {
        error("tried to find the heading between two non-neighbor hex coordinates")
    }
}

private typealias Distance = Int

class Pathfinder(
    private val origin: HexCubeCoord, private val heading: HexDirection, grid: TileGrid, blocked: Set<HexCubeCoord>
) {
    private val dist: Map<Node, Distance>
    private val order: Map<Node, Node>

    init {
        // Data structure for accumulating finished path-finding data
        val distAcc: MutableMap<Node, Distance> = HashMap()
        val orderAcc: MutableMap<Node, Node> = HashMap()

        // Temporary data structures for building the path-finding data
        val todo: MutableSet<Node> = HashSet()
        val done: MutableSet<Node> = HashSet()

        // The distance between unconnected nodes
        val max = Int.MAX_VALUE

        // Initialize with starting data
        val first = Node(origin, heading)
        distAcc[first] = 0
        todo.add(first)

        // Dijkstra's algorithm
        while (todo.isNotEmpty()) {
            // Pick the unexplored node with the smallest known distance from the origin...
            val curr = todo.reduce { nearest, candidate ->
                val distToCandidate = distAcc[candidate] ?: max
                val distToNearest = distAcc[nearest] ?: max
                if (distToCandidate < distToNearest) {
                    candidate
                } else {
                    nearest
                }
            }

            // ...then mark the node as explored
            done.add(curr)
            todo.remove(curr)

            /* Given the current Node(HexCubeCoord, HexDirection), find all of
             * the valid neighbor coordinates and compute the heading necessary
             * for a move from the current HexCubeCoord to the neighbor.
             */
            grid.allNavigableNeighbors(curr.coord, blocked)
                .map { curr.fromCoord(it) }
                .filter { curr.heading.angleTo(it.heading) < 90f }
                .forEach { succ ->
                    if (!done.contains(succ)) {
                        /* The following condition handles the case where the algorithm
                         * finds a new path to a previously visited node that is shorter
                         * than the previous path. In this case, replace the previous
                         * path with the newer, shorter path.
                         */
                        val newDistToCurr = (distAcc[curr] ?: max) + 1
                        val oldDistToSucc = distAcc[succ] ?: max
                        if (newDistToCurr < oldDistToSucc) {
                            distAcc[succ] = newDistToCurr
                            orderAcc[succ] = curr
                            todo.add(succ)
                        }
                    }
                }
        }

        dist = distAcc
        order = orderAcc
    }

    private fun nodeToPath(node: Node): Path {
        val steps: MutableList<Step> = ArrayList()
        var finish: Node? = null
        var start: Node? = node
        while (start != null) {
            if (finish != null) {
                steps.add(Step(start.coord, finish.coord, finish.heading))
            }

            if (start.coord == origin && start.heading == heading) {
                break
            } else {
                finish = start
                start = order[start]
            }
        }
        steps.reverse()
        return Path(steps)
    }

    fun toCoord(finishAt: HexCubeCoord): Path? {
        return HexDirection.values()
            .mapNotNull { heading ->
                val node = Node(finishAt, heading)
                val dist = this.dist[node]
                if (dist != null) {
                    nodeToPath(node)
                } else {
                    null
                }
            }
            .fold<Path, Path?>(null) { acc, next ->
                if (next.size < (acc?.size ?: Int.MAX_VALUE)) {
                    next
                } else {
                    acc
                }
            }
    }
}
