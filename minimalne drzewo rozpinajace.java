

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("unused")
public class MinimalSpanningTree extends Dijkstra {
	
	private final @NotNull Kruskal kruskal = new Kruskal();
	
	private final @NotNull Prim prim = new Prim();
	
	private Edges resultEdges;
	
	private @NotNull METHOD calculated;

	
	public MinimalSpanningTree() {
		super();
		resultEdges = new Edges();
		calculated = METHOD.NOT_CALCULATED;
	}

	
	public MinimalSpanningTree(@NotNull Graph graph) {
		super(graph);
		calculated = METHOD.NOT_CALCULATED;
	}

	
	public MinimalSpanningTree(@NotNull Dijkstra dijkstra) {
		super(dijkstra);
		calculated = METHOD.NOT_CALCULATED;
	}

	
	@SuppressWarnings("CopyConstructorMissesField")
	public MinimalSpanningTree(@NotNull MinimalSpanningTree MST) {
		super(MST);
		calculated = METHOD.NOT_CALCULATED;
		if (MST.calculated != METHOD.NOT_CALCULATED)
			calculateMST(MST.calculated);
	}

	
	public MinimalSpanningTree(@NotNull Graph graph, @NotNull Integer src) {
		super(graph, src);
		calculated = METHOD.NOT_CALCULATED;
	}

	
	public MinimalSpanningTree(@NotNull Graph graph, @NotNull METHOD method) {
		super(graph);
		calculated = METHOD.NOT_CALCULATED;
		calculateMST(method);
	}

	
	public MinimalSpanningTree(@NotNull Graph graph, @NotNull Integer src, @NotNull METHOD method) {
		super(graph, src);
		calculated = METHOD.NOT_CALCULATED;
		calculateMST(method);
	}

	
	public MinimalSpanningTree(@NotNull Dijkstra dijkstra, @NotNull METHOD method) {
		super(dijkstra);
		calculated = METHOD.NOT_CALCULATED;
		calculateMST(method);
	}


	@SuppressWarnings("DuplicatedCode")
	public static @NotNull MinimalSpanningTree load(@NotNull Path file) throws IOException {
		MinimalSpanningTree mst = new MinimalSpanningTree();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_16)) {
			String line;

			enum Stage {
				start, yaml, graph, vertices, edges
			}

			class Match {
				final Pattern yaml = Pattern.compile("(?<key>\\w): (?<value>.+)");
				final Pattern vertex = Pattern.compile("(?<id>\\d+)\\(\"(?<name>.*)\"\\)");
				final Pattern edge = Pattern.compile("(?<v1>\\d+) ---\\|(?<weight>\\d+\\.?\\d*)\\| (?<v2>\\d+)");

				void yaml(String line) {
					Matcher m = yaml.matcher(line);
					if (!m.matches()) {
						System.err.printf("[ERR]Unknown line: %s\n", line);
						return;
					}
					System.out.printf("\t%s: %s\n", m.group("key"), m.group("value"));
					switch (m.group("key")) {
						case "title" -> {
							if (!(m.group("value") + ".graph.mmd").equals(file.getFileName().toString())) {
								System.err.println("[WARN]Title does not match filename");
							}
						}
						case "zad" -> {
							int v = Integer.parseInt(m.group("value"));
							if (v < 3)
								System.out.println("[WARN]Format outdated");
							else if (v > 3)
								System.err.println("[WARN]Newer format detected");
						}
						case "src" -> {
							Integer id = Integer.parseInt(m.group("value"));
							System.out.printf("\tSource: %d\n", id);
							if (!id.equals(0))
								mst.src = id;
						}
						case "mst" -> {
							METHOD method = METHOD.valueOf(m.group("value"));
							System.out.printf("\tMST: %s\n", method);
							mst.calculated = method;
						}
						default ->
								System.out.printf("[INFO]Unknown property: %s\n", m.group("key"));
					}
				}

				void vertex(String line) {
					Matcher m = this.vertex.matcher(line);
					if (!m.matches()) {
						System.err.printf("[ERR]Unknown line: %s\n", line);
						return;
					}
					Integer id = Integer.parseInt(m.group("id"));
					System.out.printf("\tVertex: %d(%s)\n", id, m.group("name"));
					mst.vertices.vertices.add(mst.vertices.new Vertex(id, m.group("name")));
				}

				void edge(String line) {
					Matcher m = edge.matcher(line);
					if (!m.matches()) {
						System.err.printf("[ERR]Unknown line: %s\n", line);
						return;
					}
					Integer v1 = Integer.parseInt(m.group("v1"));
					Integer v2 = Integer.parseInt(m.group("v2"));
					Double weight = Double.parseDouble(m.group("weight"));
					System.out.printf("\tEdge: %d ---|%f| %d\n", v1, weight, v2);
					mst.edges.create(v1, v2, weight);
				}
			}

			Stage stage = Stage.start;

			Match match = new Match();

			System.out.println("Reading graph...\n");
			while ((line = reader.readLine()) != null) {
				switch (stage) {
					case start -> {
						if (line.startsWith("%% "))
							break;
						if (line.equals("---")) {
							stage = Stage.yaml;
							System.out.println("Reading properties...");
							break;
						}
						System.err.printf("[ERR]Unknown line: %s\n", line);
					}
					case yaml -> {
						if (line.startsWith("#"))
							break;
						if (line.equals("---")) {
							stage = Stage.graph;
							System.out.println("Properties read!\n");
							break;
						}
						match.yaml(line);
					}
					case graph -> {
						if (line.startsWith("%% "))
							break;
						if (line.startsWith("graph")) {
							stage = Stage.vertices;
							System.out.println("Reading structure...\n\nReading vertices...");
							break;
						}
						System.err.printf("[ERR]Unknown line: %s\n", line);
					}
					case vertices -> {
						if (line.startsWith("%% "))
							break;

						if (line.matches(match.edge.pattern())) {
							stage = Stage.edges;
							System.out.println("Vertices read!\n\nReading edges...");
							continue;
						}

						match.vertex(line);
					}
					case edges -> {
						if (line.startsWith("%% "))
							break;

						if (line.matches(match.vertex.pattern())) {
							System.err.println("[WARN]Vertex found in edges section");
							match.vertex(line);
							break;
						}

						match.edge(line);
					}
				}
			}
			System.out.println("Edges read!\n\nStructure read!\n\nGraph read!");
			if (mst.src != null)
				mst.calculateDijkstra();
			if (mst.calculated != METHOD.NOT_CALCULATED) {
				METHOD tmp = mst.calculated;
				mst.calculated = METHOD.NOT_CALCULATED;
				mst.calculateMST(tmp);
			}
		}
		return mst;
	}

	
	public void calculateMST(@NotNull METHOD method) {
		switch (method) {
			case KRUSKAL ->
					kruskal();
			case PRIM ->
					prim();
		}
	}

	
	public void kruskal() {
		kruskal.calculate();
	}

	
	public void prim() {
		prim.calculate();
	}


	@Override
	public @NotNull String mermaid() {
		if (calculated == METHOD.NOT_CALCULATED)
			throw new IllegalStateException("Minimal spanning tree not calculated, use calculate() method first or provide method");
		return String.format("graph\n%s\n%s}", vertices.mermaid(), resultEdges.mermaid());
	}


	public @NotNull String mermaid(@NotNull METHOD method) {
		calculateMST(method);
		return mermaid();
	}


	public @NotNull String mermaid(@NotNull MERMAID mermaid) {
		return mermaid == MERMAID.MST ? mermaid() : super.mermaid(mermaid.toDijkstraMermaid());
	}

	
	public @NotNull String mermaid(@NotNull MERMAID mermaid, @NotNull METHOD method) {
		return mermaid == MERMAID.MST ? mermaid(method) : super.mermaid(mermaid.toDijkstraMermaid());
	}

	
	@Override
	public @NotNull Integer addVertex(String name) {
		calculated = METHOD.NOT_CALCULATED;
		return super.addVertex(name);
	}

	
	@Override
	public @NotNull Integer addVertex() {
		calculated = METHOD.NOT_CALCULATED;
		return super.addVertex();
	}

	
	@Override
	public Integer @NotNull [] addVertex(String @NotNull ... names) {
		calculated = METHOD.NOT_CALCULATED;
		return super.addVertex(names);
	}


	@Override
	public Integer @NotNull [] addVertex(int n) {
		calculated = METHOD.NOT_CALCULATED;
		return super.addVertex(n);
	}

	
	@Override
	public void removeVertex(@NotNull Integer id) {
		calculated = METHOD.NOT_CALCULATED;
		super.removeVertex(id);
	}

	
	@Override
	public void removeVertex(@NotNull Integer @NotNull ... ids) {
		calculated = METHOD.NOT_CALCULATED;
		super.removeVertex(ids);
	}


	@Override
	public void addEdge(@NotNull Integer v1, @NotNull Integer v2, @NotNull Double weight) {
		calculated = METHOD.NOT_CALCULATED;
		super.addEdge(v1, v2, weight);
	}

	
	@Override
	public void removeEdge(@NotNull Integer v1, @NotNull Integer v2) {
		calculated = METHOD.NOT_CALCULATED;
		super.removeEdge(v1, v2);
	}

	@Override
	public void removeAllEdges(@NotNull Integer id) {
		calculated = METHOD.NOT_CALCULATED;
		super.removeAllEdges(id);
	}

	
	@Override
	public void removeAllEdges(@NotNull Integer @NotNull ... ids) {
		calculated = METHOD.NOT_CALCULATED;
		super.removeAllEdges(ids);
	}

	
	@Override
	public void setEdgeWeight(@NotNull Integer v1, @NotNull Integer v2, @NotNull Double weight) {
		calculated = METHOD.NOT_CALCULATED;
		super.setEdgeWeight(v1, v2, weight);
	}

	
	@Override
	public void save(@NotNull String name) throws IOException {
		Path file = Path.of(name + ".graph.mmd");
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_16)) {
			if (src == null)
				src = 0;
			String s = String.format("""
					---
					title: %s
					zad: 3
					src: %d
					mst: %s
					---
					%s
					""", name, src, calculated, mermaid(Graph.MERMAID.GRAPH));
			writer.write(s);
			if (src == 0)
				src = null;
		}
	}

	
	public enum MERMAID {
		VERTICES, EDGES, GRAPH, DIJKSTRA, MST;

		
		public Dijkstra.MERMAID toDijkstraMermaid() {
			return switch (this) {
				case VERTICES ->
						Dijkstra.MERMAID.VERTICES;
				case EDGES ->
						Dijkstra.MERMAID.EDGES;
				case GRAPH ->
						Dijkstra.MERMAID.GRAPH;
				case DIJKSTRA ->
						Dijkstra.MERMAID.DIJKSTRA;
				default ->
						throw new IllegalArgumentException("Unexpected value: " + this);
			};
		}

	
		public Graph.MERMAID toGraphMermaid() {
			return toDijkstraMermaid().toGraphMermaid();
		}
	}

	
	public enum METHOD {
		KRUSKAL, PRIM, NOT_CALCULATED
	}

te class Kruskal {
		
		PriorityQueue<Edges.Edge> edgesQueue;
		
		Integer[] parent;

		
		public void calculate() {
			if (calculated == METHOD.KRUSKAL)
				return;

			resultEdges = new Edges();

			edgesQueue = new PriorityQueue<>(Comparator.comparingDouble(Edges.Edge::getWeight));
			edgesQueue.addAll(edges.getEdges());

			parent = new Integer[vertices.getIds().size()];
			for (int i = 0; i < parent.length; i++)
				parent[i] = i;

			while (!edgesQueue.isEmpty()) {
				Edges.Edge edge = edgesQueue.poll();

				int srcParent = find(edge.getV1());
				int destParent = find(edge.getV2());

				if (srcParent != destParent) {
					resultEdges.create(edge.getV1(), edge.getV2(), edge.getWeight());
					union(srcParent, destParent);
				}
			}

			calculated = METHOD.KRUSKAL;
		}

	
		private int find(@NotNull Integer vertex) {
			if (!parent[vertex].equals(vertex))
				parent[vertex] = find(parent[vertex]);
			return parent[vertex];
		}

		
		private void union(@NotNull Integer src, @NotNull Integer dest) {
			int srcParent = find(src);
			int destParent = find(dest);
			parent[srcParent] = destParent;
		}
	}

	
	private class Prim {
	
		PriorityQueue<Edges.Edge> edgesQueue;
	
		Double[] key;

	
		public void calculate() {
			if (calculated == METHOD.PRIM)
				return;

			resultEdges = new Edges();

			edgesQueue = new PriorityQueue<>(Comparator.comparingDouble(Edges.Edge::getWeight));
			boolean[] mstSet = new boolean[vertices.getIds().size()];

			key = new Double[vertices.getIds().size()];
			Arrays.fill(key, Double.MAX_VALUE);

			key[0] = 0.0;

			for (int count = 0; count < vertices.getIds().size() - 1; count++) {
				int u = -1;
				for (int i = 0; i < vertices.getIds().size(); i++)
					if (!mstSet[i] && (u == -1 || key[i] < key[u]))
						u = i;

				mstSet[u] = true;

				for (int v = 0; v < vertices.getIds().size(); v++) {
					Double weight = edges.getWeight(u, v);
					if (!mstSet[v] && weight < key[v]) {
						key[v] = weight;
						resultEdges.create(u, v, weight);
					}
				}
			}

			calculated = METHOD.PRIM;
		}
	}
}
