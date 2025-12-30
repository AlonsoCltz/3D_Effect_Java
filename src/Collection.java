import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Collection {
    private final List<CollectableObject> bag;
    private final List<CollectableObject> worldObjects;

    public Collection() {
        bag = new ArrayList<>();
        worldObjects = new ArrayList<>();
    }

    public void loadTestObjects() {
        if (!worldObjects.isEmpty()) return;
        worldObjects.add(new CollectableObject(1, 5, "Gold Coin", Color.YELLOW));
        worldObjects.add(new CollectableObject(15, 20, "Silver Key", Color.LIGHT_GRAY));
    }

    public List<CollectableObject> getWorldObjects() {
        return Collections.unmodifiableList(worldObjects);
    }

    public List<CollectableObject> getBag() {
        return Collections.unmodifiableList(bag);
    }

    public void addWorldObject(CollectableObject object) {
        if (object != null) {
            worldObjects.add(object);
        }
    }

    public void collect(CollectableObject object) {
        if (object == null || object.isCollected()) return;
        object.setCollected(true);
        bag.add(object);
    }
}
