package be.enceladus.scan2ereader.core.processing.aggregation;

import be.enceladus.scan2ereader.core.ocr.model.TextZone;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

public class Aggregator<T extends TextZone> {

    private final static Logger logger = Logger.getLogger(Aggregator.class.getName());

    private List<T> toGroup;
    private BiPredicate<T, List<T>> belongsToGroupPredicate;

    public Aggregator(BiPredicate<T, List<T>> belongsToGroupPredicate, List<T> toGroup) {
        this.belongsToGroupPredicate = belongsToGroupPredicate;
        this.toGroup = toGroup;
    }

    public List<List<T>> buildGroups() {
        List<List<T>> groups = new ArrayList<>();

        for (T objectToGroup : toGroup) {
            List<T> group = findGroupBelongsTo(objectToGroup, groups);
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            group.add(objectToGroup);
        }
        return groups;
    }

    private List<T> findGroupBelongsTo(T objectToGroup, List<List<T>> groups) {
        return groups
                .stream()
                .filter(group -> belongsToGroupPredicate.test(objectToGroup, group))
                .findFirst()
                .orElse(null);
    }
}

