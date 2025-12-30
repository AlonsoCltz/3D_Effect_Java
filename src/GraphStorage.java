import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphStorage {
	private final List<int[][]> graphs;

	private GraphStorage() {
		graphs = new ArrayList<>();
		graphs.add(buildDefaultMap());
		graphs.add(buildDefaultMaze());
	}

	public static GraphStorage getInstance() {
		return Holder.INSTANCE;
	}

	public int getGraphCount() {
		return graphs.size();
	}

	public int[][] getGraph(int id) {
		if (id < 0 || id >= graphs.size()) {
			return null;
		}
		return graphs.get(id);
	}

	public List<int[][]> getAll() {
		return Collections.unmodifiableList(graphs);
	}

	private int[][] buildDefaultMap() {
		int size = 30;
		int[][] grid = new int[size][size];

		for (int i = 0; i < size; i++) {
			grid[0][i] = 1;
			grid[size - 1][i] = 1;
			grid[i][0] = 2;
			grid[i][size - 1] = 2;
		}

		int start = 12, end = 17;
		for (int j = start; j < end; j++) grid[start][j] = 3;
		grid[start][end] = 4;
		for (int i = start + 1; i < end; i++) {
			grid[i][start] = 4;
			grid[i][end] = 4;
		}
		grid[end][start] = 4;
		for (int j = start + 1; j <= end; j++) grid[end][j] = 3;

		return grid;
	}
	private int[][] buildDefaultMaze() {
		int size = 150;
		int[][] grid = new int[size][size];

		Path mazePath = Paths.get("mapStorage", "DefaultMaze.txt");
		// Fallback if the game is launched from the project root's parent directory.
		if (!Files.exists(mazePath)) {
			mazePath = Paths.get("DefaultMaze.txt");
		}

		try (BufferedReader reader = Files.newBufferedReader(mazePath)) {
			String line;
			int row = 0;
			while ((line = reader.readLine()) != null && row < size) {
				String[] tokens = line.trim().split("\\s+");
				int col = 0;
				for (String token : tokens) {
					String trimmed = token.trim();
					if (trimmed.isEmpty()) continue;
					if (col >= size) break;
					grid[row][col++] = Integer.parseInt(trimmed);
				}
				row++;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load maze from " + mazePath.toAbsolutePath(), e);
		}

		return grid;
	}

	private static class Holder {
		private static final GraphStorage INSTANCE = new GraphStorage();
	}
}
